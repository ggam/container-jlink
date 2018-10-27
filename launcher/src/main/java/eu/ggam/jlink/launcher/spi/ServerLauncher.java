package eu.ggam.jlink.launcher.spi;

/**
 *
 * @author Guillermo González de Agüero
 */
public interface ServerLauncher {

    public void launch(WebAppModule webAppModule) throws Exception;
}
