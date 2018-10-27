package eu.ggam.jlink.launcher;

import java.lang.module.Configuration;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

/**
 *
 * @author Guillermo González de Agüero
 */
public class Bootstrap {

    private static final Logger LOGGER = Logger.getLogger(Bootstrap.class.getName());

    private static final Path SERVER_IMPL = Paths.get("lib", "runtime-impl");

    public static void main(String... args) throws NoSuchMethodException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, InvocationTargetException {
        ModuleFinder serverFinder = ModuleFinder.of(SERVER_IMPL);
        Set<String> serverModuleNames = serverFinder.findAll().
                stream().
                map(ModuleReference::descriptor).
                map(ModuleDescriptor::name).
                collect(toSet());
        
        LOGGER.log(Level.INFO, "Server modules at {0}:\n{1}", new Object[]{SERVER_IMPL, serverModuleNames.stream().map(m -> "- " + m + "\n").collect(joining())});

        serverModuleNames.add(Bootstrap.class.getModule().getName());

        Path thisModuleLocation = Paths.get(ModuleLayer.
                boot().
                configuration().
                findModule(Bootstrap.class.getModule().getName()).
                get().
                reference().
                location().
                get());

        Configuration configuration = ModuleLayer.boot().configuration().resolve(ModuleFinder.compose(serverFinder, ModuleFinder.of(thisModuleLocation)), ModuleFinder.ofSystem(), serverModuleNames);
        ModuleLayer serverLayer = ModuleLayer.defineModulesWithOneLoader(configuration, List.of(ModuleLayer.boot()), ClassLoader.getSystemClassLoader()).layer();

        Module thisModuleServerLayer = serverLayer.findModule(Bootstrap.class.getModule().getName()).get();

        if (Bootstrap.class.getModule().getLayer() == thisModuleServerLayer.getLayer()) {
            throw new IllegalStateException("Bootstrap Module not loaded from server layer!!");
        }

        Class<?> forName = Class.forName(thisModuleServerLayer, ServerFinder.class.getName());

        Object newInstance = forName.getConstructor().newInstance();

        String warModuleName = System.getProperty("eu.ggam.jlink.war_module");

        forName.getMethod("find", String.class).invoke(newInstance, warModuleName);
    }
}
