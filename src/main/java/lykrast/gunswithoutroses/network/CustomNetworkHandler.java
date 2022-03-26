package lykrast.gunswithoutroses.network;

import lykrast.gunswithoutroses.GunsWithoutRoses;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.event.EventNetworkChannel;

import java.util.function.Predicate;
import java.util.function.Supplier;

public class CustomNetworkHandler {

    private static final Supplier<String> CONST_1 = () -> "1";
    private static final Predicate<String> TRUE = e -> true;
    private final Supplier<String> networkProtocolVersion = CONST_1;
    private final Predicate<String> clientAcceptedVersions = TRUE;
    private final Predicate<String> serverAcceptedVersions = TRUE;

    public void init() {
        NetworkRegistry.ChannelBuilder parent = NetworkRegistry.ChannelBuilder.named(GunsWithoutRoses.rl("custom_channel"));

        EventNetworkChannel channel = parent
                .networkProtocolVersion(networkProtocolVersion)
                .clientAcceptedVersions(clientAcceptedVersions)
                .serverAcceptedVersions(serverAcceptedVersions)
                .eventNetworkChannel();

        channel.registerObject(new ClientPacketHandler());
    }
}
