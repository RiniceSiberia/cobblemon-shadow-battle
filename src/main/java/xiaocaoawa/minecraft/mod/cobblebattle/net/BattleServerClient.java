package xiaocaoawa.minecraft.mod.cobblebattle.net;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.InflaterInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import xiaocaoawa.minecraft.mod.cobblebattle.config.CobbleBattleConfig;

public final class BattleServerClient {
   private static final Logger LOGGER = LoggerFactory.getLogger("CobbleBattle/Net");
   private static final Gson GSON = new Gson();
   private static final int HEADER_BYTES = 4;
   private static final int BUFFER_BYTES = 16384;
   private final CobbleBattleConfig config;
   private final Consumer<JsonObject> inboundHandler;
   private final Runnable onConnected;
   private final Consumer<String> onDisconnected;
   private final AtomicBoolean running = new AtomicBoolean(false);
   private final AtomicBoolean reconnectNow = new AtomicBoolean(false);
   private volatile boolean wanted;
   private volatile String refusedReason;
   private volatile String closingBecause;
   private final Object gate = new Object();
   private volatile Consumer<String> onConnectFailed = why -> {};
   private volatile Thread loopThread;
   private final AtomicInteger refCounter = new AtomicInteger(0);
   private final Object writeLock = new Object();
   private volatile Socket socket;
   private volatile OutputStream out;
   private volatile Deflater deflater;
   private volatile Thread readerThread;
   private volatile boolean handshaken;

   public BattleServerClient(CobbleBattleConfig config, Consumer<JsonObject> inboundHandler, Runnable onConnected, Consumer<String> onDisconnected) {
      this.config = config;
      this.inboundHandler = inboundHandler;
      this.onConnected = onConnected;
      this.onDisconnected = onDisconnected;
   }

   public boolean isConnected() {
      Socket s = this.socket;
      return s != null && s.isConnected() && !s.isClosed();
   }

   public boolean isHandshaken() {
      return this.handshaken && this.isConnected();
   }

   public void setHandshaken(boolean value) {
      this.handshaken = value;
   }

   public int nextRef() {
      return this.refCounter.incrementAndGet();
   }

   public void start() {
      if (this.running.compareAndSet(false, true)) {
         Thread thread = new Thread(this::connectLoop, "CobbleBattle-Net");
         thread.setDaemon(true);
         thread.start();
      }
   }

   public void stop() {
      this.running.set(false);
      this.wanted = false;
      this.closeSocket("shutdown");
      Thread reader = this.readerThread;
      if (reader != null) {
         reader.interrupt();
      }

      this.wake();
   }

   public void connect() {
      this.refusedReason = null;
      if (!this.wanted) {
         this.wanted = true;
         this.wake();
      }
   }

   public void disconnect(String why) {
      this.wanted = false;
      this.closingBecause = why;
      this.closeSocket(why);
   }

   public void suspend(String why) {
      this.refusedReason = why;
      this.wanted = false;
      this.closingBecause = why;
      this.closeSocket(why);
   }

   public void setOnConnectFailed(Consumer<String> handler) {
      this.onConnectFailed = handler;
   }

   public String refusedReason() {
      return this.refusedReason;
   }

   public boolean isWanted() {
      return this.wanted;
   }

   private void wake() {
      synchronized (this.gate) {
         this.gate.notifyAll();
      }

      Thread loop = this.loopThread;
      if (loop != null) {
         loop.interrupt();
      }
   }

   public void reconnect(String why) {
      if (this.running.get()) {
         LOGGER.info("Dropping the battle server connection to reconnect: {}", why);
         this.refusedReason = null;
         this.wanted = true;
         this.reconnectNow.set(true);
         this.closingBecause = why;
         this.closeSocket(why);
         this.wake();
      }
   }

   private void connectLoop() {
      this.loopThread = Thread.currentThread();
      int attempt = 0;

      while (this.running.get()) {
         if (!this.wanted) {
            attempt = 0;
            synchronized (this.gate) {
               while (this.running.get() && !this.wanted) {
                  try {
                     this.gate.wait();
                  } catch (InterruptedException var13) {
                     Thread.interrupted();
                  }
               }
            }
         } else {
            if (this.reconnectNow.compareAndSet(true, false)) {
               attempt = 0;
            }

            Thread.interrupted();

            try {
               LOGGER.info("Connecting to battle server {}:{} (attempt {})", new Object[]{this.config.serverHost, this.config.serverPort, ++attempt});
               Socket s = new Socket();
               s.setTcpNoDelay(true);
               s.connect(new InetSocketAddress(this.config.serverHost, this.config.serverPort), this.config.connectTimeoutMs);
               if (!this.wanted) {
                  s.close();
                  continue;
               }

               this.socket = s;
               this.out = new BufferedOutputStream(s.getOutputStream(), 16384);
               this.deflater = null;
               this.handshaken = false;
               attempt = 0;
               LOGGER.info("Connected to battle server");
               this.onConnected.run();
               this.readerThread = Thread.currentThread();
               this.readFrames(new BufferedInputStream(s.getInputStream(), 16384));
            } catch (IOException var16) {
               if (this.running.get() && this.wanted) {
                  LOGGER.warn("Battle server connection problem: {}", var16.getMessage());
                  if (!this.handshaken) {
                     this.onConnectFailed.accept(var16.getMessage());
                  }
               }
            } finally {
               boolean handshook = this.handshaken;
               this.handshaken = false;
               String because = this.closingBecause;
               this.closingBecause = null;
               this.closeSocket("reader ended");
               if (handshook) {
                  this.onDisconnected.accept(because != null ? because : "connection lost");
               }
            }

            if (!this.running.get()) {
               break;
            }

            if (this.wanted) {
               long delay = Math.min((long)this.config.reconnectMaxDelayMs, (long)this.config.reconnectBaseDelayMs * Math.max(1, attempt));

               try {
                  Thread.sleep(delay);
               } catch (InterruptedException var14) {
                  if (!this.running.get()) {
                     Thread.currentThread().interrupt();
                     break;
                  }

                  Thread.interrupted();
               }
            }
         }
      }

      LOGGER.info("Battle server client stopped");
   }

   private void readFrames(BufferedInputStream base) throws IOException {
      DataInputStream in = new DataInputStream(base);
      byte[] header = new byte[4];

      while (this.running.get()) {
         in.readFully(header);
         int length = (header[0] & 255) << 24 | (header[1] & 255) << 16 | (header[2] & 255) << 8 | header[3] & 255;
         if (length < 0 || length > this.config.maxFrameBytes) {
            throw new IOException("Frame of " + length + " bytes exceeds the configured maximum");
         }

         byte[] payload = new byte[length];
         in.readFully(payload);

         JsonObject message;
         try {
            JsonElement parsed = JsonParser.parseString(new String(payload, StandardCharsets.UTF_8));
            if (!parsed.isJsonObject()) {
               LOGGER.warn("Ignoring a non-object frame from the battle server");
               continue;
            }

            message = parsed.getAsJsonObject();
         } catch (RuntimeException var9) {
            throw new IOException("Malformed JSON frame: " + var9.getMessage(), var9);
         }

         if ("hello_ack".equals(str(message, "t", ""))) {
            this.startDeflating();
            in = new DataInputStream(new InflaterInputStream(base));
         }

         try {
            this.inboundHandler.accept(message);
         } catch (RuntimeException var8) {
            LOGGER.error("Handler for '{}' threw", message.has("t") ? message.get("t").getAsString() : "?", var8);
         }
      }
   }

   private void startDeflating() {
      synchronized (this.writeLock) {
         OutputStream current = this.out;
         Socket s = this.socket;
         if (current != null && s != null && this.deflater == null) {
            try {
               current.flush();
               Deflater d = new Deflater(-1);
               this.out = new BufferedOutputStream(new DeflaterOutputStream(s.getOutputStream(), d, 16384, true), 16384);
               this.deflater = d;
               LOGGER.info("The battle server link is deflated");
            } catch (IOException var6) {
               LOGGER.warn("Could not switch the link to deflate: {}", var6.getMessage());
               this.closeSocket("compression failed");
            }
         }
      }
   }

   public boolean send(JsonObject message) {
      return !this.handshaken ? false : this.write(message);
   }

   public boolean sendHandshake(JsonObject message) {
      return this.write(message);
   }

   private boolean write(JsonObject message) {
      if (!this.isConnected()) {
         return false;
      } else {
         byte[] payload = GSON.toJson(message).getBytes(StandardCharsets.UTF_8);
         byte[] frame = new byte[4 + payload.length];
         frame[0] = (byte)(payload.length >>> 24);
         frame[1] = (byte)(payload.length >>> 16);
         frame[2] = (byte)(payload.length >>> 8);
         frame[3] = (byte)payload.length;
         System.arraycopy(payload, 0, frame, 4, payload.length);
         synchronized (this.writeLock) {
            OutputStream target = this.out;
            if (target == null) {
               return false;
            } else {
               boolean var10000;
               try {
                  target.write(frame);
                  target.flush();
                  var10000 = true;
               } catch (IOException var8) {
                  LOGGER.warn("Failed to write to the battle server: {}", var8.getMessage());
                  this.closeSocket("write failed");
                  return false;
               }

               return var10000;
            }
         }
      }
   }

   private void closeSocket(String reason) {
      Socket s = this.socket;
      this.socket = null;
      synchronized (this.writeLock) {
         this.out = null;
         Deflater d = this.deflater;
         if (d != null) {
            this.deflater = null;
            d.end();
         }
      }

      if (s != null) {
         try {
            s.close();
         } catch (IOException var6) {
         }

         LOGGER.debug("Socket closed ({})", reason);
      }
   }

   public static JsonObject msg(String type) {
      JsonObject object = new JsonObject();
      object.addProperty("t", type);
      return object;
   }

   public static String str(JsonObject object, String key, String fallback) {
      JsonElement element = object.get(key);
      return element != null && !element.isJsonNull() ? element.getAsString() : fallback;
   }

   public static int integer(JsonObject object, String key, int fallback) {
      JsonElement element = object.get(key);
      return element != null && !element.isJsonNull() ? element.getAsInt() : fallback;
   }

   public static boolean bool(JsonObject object, String key, boolean fallback) {
      JsonElement element = object.get(key);
      return element != null && !element.isJsonNull() ? element.getAsBoolean() : fallback;
   }

   public static long longer(JsonObject object, String key, long fallback) {
      JsonElement element = object.get(key);
      return element != null && !element.isJsonNull() ? element.getAsLong() : fallback;
   }
}
