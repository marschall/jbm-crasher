package com.github.marschall.jbm.crasher;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import org.jboss.modules.ClassSpec;
import org.jboss.modules.ModuleDependencySpecBuilder;
import org.jboss.modules.ModuleFinder;
import org.jboss.modules.ModuleIdentifier;
import org.jboss.modules.ModuleLoadException;
import org.jboss.modules.ModuleLoader;
import org.jboss.modules.ModuleSpec;
import org.jboss.modules.ModuleSpec.Builder;
import org.jboss.modules.PackageSpec;
import org.jboss.modules.Resource;
import org.jboss.modules.ResourceLoader;
import org.jboss.modules.ResourceLoaderSpec;
import org.jboss.modules.Version;

public final class MemoryModuleFinder implements ModuleFinder {

  static final String RUNNABLE_EVENT = "com.github.marschall.jbm.crasher.RunnableEvent";

  static final String JFR_RUNNABLE = "com.github.marschall.jbm.crasher.JfrRunnable";
  
  static final String MODULE_NAME = "jbm-crasher";

  @Override
  public ModuleSpec findModule(String name, ModuleLoader delegateLoader) throws ModuleLoadException {
    if (!name.equals(MODULE_NAME)) {
      return null;
    }
    
    Builder builder = ModuleSpec.build(name);

    builder.setVersion(Version.parse("1.0.0"));

    builder.addDependency(new ModuleDependencySpecBuilder()
        .setName("jdk.jfr")
        .build());

    byte[] runnableClass = loadBytecode(JFR_RUNNABLE);
    byte[] eventClass = loadBytecode(RUNNABLE_EVENT);
    ResourceLoader resourceLoader = new MemoryResourceLoader(runnableClass, eventClass);
    builder.addResourceRoot(ResourceLoaderSpec.createResourceLoaderSpec(resourceLoader));
    
    builder.setMainClass(JFR_RUNNABLE);

    return builder.create();
  }

  static byte[] loadBytecode(String className) {
    String resource = toResourceName(className);
    ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    try (InputStream inputStream = MemoryModuleFinder.class.getClassLoader().getResourceAsStream(resource)) {
      inputStream.transferTo(buffer);
    } catch (IOException e) {
      throw new UncheckedIOException("could not get bytecode of class:" + className, e);
    }
    return buffer.toByteArray();
  }

  private static String toResourceName(String className) {
    return className.replace('.', '/') + ".class";
  }

  @Override
  public ModuleSpec findModule(ModuleIdentifier moduleIdentifier, ModuleLoader delegateLoader)
      throws ModuleLoadException {
    return findModule(moduleIdentifier.toString(), delegateLoader);
  }

  @Override
  public String toString() {
    return "Memory Module Finder";
  }

  static final class MemoryResourceLoader implements ResourceLoader {

    static final String RUNNABLE_EVENT_RESOURCE = RUNNABLE_EVENT.replace('.', '/') + ".java";

    static final String JFR_RUNNABLE_RESOURCE = JFR_RUNNABLE.replace('.', '/') + ".java";

    private final byte[] runnableClass;

    private final byte[] eventClass;

    MemoryResourceLoader(byte[] runnableClass, byte[] eventClass) {
      Objects.requireNonNull(runnableClass, "runnableClass");
      Objects.requireNonNull(eventClass, "eventClass");
      this.runnableClass = runnableClass;
      this.eventClass = eventClass;
    }

    @Override
    public ClassSpec getClassSpec(String fileName) throws IOException {
      if (fileName.equals(RUNNABLE_EVENT_RESOURCE)) {
        ClassSpec classSpec = new ClassSpec();
        classSpec.setBytes(this.eventClass);
        return classSpec;
      } else if (fileName.equals(JFR_RUNNABLE_RESOURCE)) {
        ClassSpec classSpec = new ClassSpec();
        classSpec.setBytes(this.runnableClass);
        return classSpec;
      } else {
        return null;
      }
    }

    @Override
    public PackageSpec getPackageSpec(String name) throws IOException {
      PackageSpec packageSpec = new PackageSpec();
      return packageSpec;
    }

    @Override
    public Resource getResource(String name) {
      if (name.equals(RUNNABLE_EVENT_RESOURCE)) {
        return new MemoryResource(name, this.eventClass);
      } else if (name.equals(JFR_RUNNABLE_RESOURCE)) {
        return new MemoryResource(name, this.runnableClass);
      } else {
        return null;
      }
    }

    @Override
    public String getLibrary(String name) {
      return null;
    }

    @Override
    public Collection<String> getPaths() {
      // TODO cache
      List<String> paths = new ArrayList<>();

      List<String> elements = List.of("com", "github", "marschall", "jbm", "crasher");
      for (int i = 1; i <= elements.size(); i++) {
        paths.add(String.join("/", elements.subList(0, i)));
      }

//      return paths;
      return List.of("com/github/marschall/jbm/crasher");
    }

  }

  static final class MemoryResource implements Resource {

    private final String name;
    private final byte[] data;

    MemoryResource(String name, byte[] data) {
      this.name = name;
      this.data = data;
    }

    @Override
    public String getName() {
      return this.name;
    }

    @Override
    public URL getURL() {
      //      return new URL(null, "", MemoryURLStreamHandler.INSTANCE);
      // TODO Auto-generated method stub
      return null;
    }

    @Override
    public InputStream openStream() throws IOException {
      return new ByteArrayInputStream(this.data);
    }

    @Override
    public long getSize() {
      return this.data.length;
    }

  }

  static final class MemoryURLStreamHandler extends URLStreamHandler {

    static final URLStreamHandler INSTANCE = new MemoryURLStreamHandler();

    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      //      try {
      //        URI uri = url.toURI();
      //        Path path = Paths.get(uri);
      //        return new PathURLConnection(url, path);
      //      } catch (URISyntaxException e) {
      //        throw new IOException("invalid URL", e);
      //      }
      throw new IOException("invalid URL");
    }

  }

  static final class MemoryURLConnection extends URLConnection {

    private final byte[] data;

    MemoryURLConnection(URL url, byte[] data) {
      super(url);
      this.data = data;
    }

    @Override
    public void connect() throws IOException {
      // nothing to do

    }

    @Override
    public long getContentLengthLong() {
      return this.data.length;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      return new ByteArrayInputStream(this.data);
    }

    @Override
    public long getLastModified() {
      return 0;
    }

    public String getContentType() {
      return null;
    }

    public String getContentEncoding() {
      return null;
    }

  }

}
