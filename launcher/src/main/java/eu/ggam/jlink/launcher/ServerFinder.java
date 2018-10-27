package eu.ggam.jlink.launcher;

import eu.ggam.jlink.launcher.spi.ServerLauncher;
import eu.ggam.jlink.launcher.spi.WebAppModule;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 *
 * @author Guillermo González de Agüero
 */
public class ServerFinder {

    public void find(String warModuleName) {
        System.out.println("Boot layer: " + ModuleLayer.boot().hashCode());
        System.out.println("ServerFinder layer: " + getClass().getModule().getLayer().hashCode());
        System.out.println("Same layer: " + getClass().getModule().getLayer().equals(ModuleLayer.boot()));

        Optional<ServerLauncher> findFirst = ServiceLoader.load(ServerLauncher.class, getClass().getModule().getClassLoader()).findFirst();

        if (!findFirst.isPresent()) {
            throw new IllegalStateException("There's no ServerLauncher implementation!");
        }

        try {
            WebAppModule webAppModule = WebAppModule.of(ModuleLayer.boot().
                    findModule(warModuleName).
                    get());

            findFirst.get().launch(webAppModule);
        } catch (Exception ex) {
            throw new IllegalStateException("Error getting " + warModuleName + " WAR module to work");
        }
    }
}
