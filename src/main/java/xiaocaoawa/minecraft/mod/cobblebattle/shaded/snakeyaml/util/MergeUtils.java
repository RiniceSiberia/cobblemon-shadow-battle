package xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.nodes.MappingNode;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.nodes.Node;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.nodes.NodeTuple;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.nodes.ScalarNode;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.nodes.SequenceNode;
import xiaocaoawa.minecraft.mod.cobblebattle.shaded.snakeyaml.nodes.Tag;

public abstract class MergeUtils {
   public abstract MappingNode asMappingNode(Node var1);

   public List<NodeTuple> flatten(MappingNode node) {
      List<NodeTuple> toProcess = node.getValue();
      List<NodeTuple> result = toProcess;
      boolean process = true;

      while (process) {
         process = false;
         List<NodeTuple> updated = new ArrayList<>(toProcess.size());
         Set<String> keys = new HashSet<>(toProcess.size());
         List<NodeTuple> merges = new ArrayList<>(2);

         for (NodeTuple tuple : toProcess) {
            Node keyNode = tuple.getKeyNode();
            if (keyNode.getTag().equals(Tag.MERGE)) {
               merges.add(tuple);
            } else {
               updated.add(tuple);
               if (keyNode instanceof ScalarNode) {
                  ScalarNode sNode = (ScalarNode)keyNode;
                  keys.add(sNode.getValue());
               }
            }
         }

         for (NodeTuple tuplex : merges) {
            Node valueNode = tuplex.getValueNode();
            if (valueNode instanceof SequenceNode) {
               SequenceNode seqNode = (SequenceNode)valueNode;

               for (Node ref : seqNode.getValue()) {
                  MappingNode mergable = this.asMappingNode(ref);
                  process = process || mergable.isMerged();
                  Tuple<List<NodeTuple>, Set<String>> filtered = this.filter(mergable.getValue(), keys);
                  updated.addAll(filtered._1());
                  keys.addAll(filtered._2());
               }
            } else {
               MappingNode mergable = this.asMappingNode(valueNode);
               process = process || mergable.isMerged();
               Tuple<List<NodeTuple>, Set<String>> filtered = this.filter(mergable.getValue(), keys);
               updated.addAll(filtered._1());
               keys.addAll(filtered._2());
            }
         }

         result = updated;
         if (process) {
            toProcess = updated;
         }
      }

      return result;
   }

   private Tuple<List<NodeTuple>, Set<String>> filter(List<NodeTuple> mergables, Set<String> filter) {
      int size = mergables.size();
      Set<String> keys = new HashSet<>(size);
      List<NodeTuple> result = new ArrayList<>(size);

      for (NodeTuple tuple : mergables) {
         Node key = tuple.getKeyNode();
         if (key instanceof ScalarNode) {
            ScalarNode sNode = (ScalarNode)key;
            String nodeValue = sNode.getValue();
            if (!filter.contains(nodeValue)) {
               result.add(tuple);
               keys.add(nodeValue);
            }
         } else {
            result.add(tuple);
         }
      }

      return new Tuple<>(result, keys);
   }
}
