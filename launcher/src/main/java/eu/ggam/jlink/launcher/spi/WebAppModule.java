package eu.ggam.jlink.launcher.spi;

import java.util.Set;

/**
 *
 * @author Guillermo González de Agüero
 */
public final class WebAppModule {

    private final Module module;
    private final Set<JarLibraryModule> libraries;

    private WebAppModule(Module module, Set<JarLibraryModule> libraries) {
        this.module = module;
        this.libraries = libraries;
    }

    public static WebAppModule of(Module warModule) {
        return new WebAppModule(warModule, Set.of());
    }

    public Module getModule() {
        return module;
    }

    public Set<JarLibraryModule> getLibraries() {
        return libraries;
    }

}
