package xiaocaoawa.minecraft.mod.cobblebattle.command

import com.mojang.brigadier.CommandDispatcher
import net.minecraft.commands.CommandSourceStack
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test

class CommandTreeContractTest {
    @Test
    fun `根命令注册全部子命令和排行参数`() {
        val dispatcher = CommandDispatcher<CommandSourceStack>()
        MainCommand.register(dispatcher)

        val root = dispatcher.root.getChild("cbattle")
        assertNotNull(root)
        assertEquals(
            setOf("open", "logout", "join", "leave", "check", "status", "reload"),
            root.children.map { it.name }.toSet(),
        )
        assertEquals(setOf("ranked"), root.getChild("open").children.map { it.name }.toSet())
        assertEquals(setOf("ranked"), root.getChild("join").children.map { it.name }.toSet())
    }

    @Test
    fun `只有重载子命令要求二级权限`() {
        val subcommands = listOf(
            SubOpenCommand(),
            SubLogoutCommand(),
            SubJoinCommand(),
            SubLeaveCommand(),
            SubCheckCommand(),
            SubStatusCommand(),
            SubReloadCommand(),
        )
        assertEquals(listOf("open", "logout", "join", "leave", "check", "status", "reload"), subcommands.map { it.name() })
        assertEquals(listOf(0, 0, 0, 0, 0, 0, 2), subcommands.map { it.permissionLevel() })
    }
}
