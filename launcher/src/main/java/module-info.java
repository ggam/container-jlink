module eu.ggam.jlink.launcher {
    requires java.logging;
    
    exports eu.ggam.jlink.launcher;
    exports eu.ggam.jlink.launcher.spi;
    
    uses eu.ggam.jlink.launcher.spi.ServerLauncher;
}
