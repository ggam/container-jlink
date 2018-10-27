package eu.ggam.jlink.generator;

import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.collection.DependencySelector;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;

/**
 *
 * @author Guillermo González de Agüero
 */
public class MavenRepositoryHelper {

    private final RepositorySystem repoSystem;
    private final DefaultRepositorySystemSession session;

    public MavenRepositoryHelper(String m2Home) {

        DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        this.session = new DefaultRepositorySystemSession();

        this.repoSystem = locator.getService(RepositorySystem.class);

        session.setLocalRepositoryManager(repoSystem.newLocalRepositoryManager(session, new LocalRepository(m2Home + "/repository")));

        session.setDependencySelector(new DependencySelector() {
            @Override
            public boolean selectDependency(Dependency dependency) {
                return !"test".equals(dependency.getScope());
            }

            @Override
            public DependencySelector deriveChildSelector(DependencyCollectionContext context) {
                return this;
            }
        });

    }

    public Stream<Artifact> getDependencies(String mavenCoords) {
        try {
            Dependency dependency = new Dependency(new DefaultArtifact(mavenCoords), "compile");

            CollectRequest collectRequest = new CollectRequest();
            collectRequest.setRoot(dependency);
            collectRequest.addRepository(new RemoteRepository.Builder("central", "default", "http://repo1.maven.org/maven2/").build());

            DependencyNode node = repoSystem.collectDependencies(session, collectRequest).getRoot();

            DependencyRequest dependencyRequest = new DependencyRequest();
            dependencyRequest.setRoot(node);

            DependencyResult resolveDependencies = repoSystem.resolveDependencies(session, dependencyRequest);
            return resolveDependencies.getArtifactResults().
                    stream().
                    map(ArtifactResult::getArtifact);
        } catch (DependencyCollectionException | DependencyResolutionException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public Path getLocalPath(String mavenCoords) {
        try {
            ArtifactRequest artifactRequest = new ArtifactRequest();
            artifactRequest.setArtifact(new DefaultArtifact(mavenCoords));
            
            ArtifactResult resolveArtifact = repoSystem.resolveArtifact(session, artifactRequest);
            return resolveArtifact.getArtifact().
                    getFile().
                    toPath().
                    toAbsolutePath();
        } catch (ArtifactResolutionException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

}
