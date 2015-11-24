/*
 * Copyright 2015 Marco Simport java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package fr.ms.instrument;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

/**
 *
 * @see <a href="http://marcosemiao4j.wordpress.com">Marco4J</a>
 *
 *
 * @author Marco Semiao
 *
 */
public class JarTransformer {

    private final static String CLASS_EXTENSION = ".class";

    private final ClassLoader loader;
    private final List<File> sources;
    private final List<ClassFileTransformer> classFileTransformers;

    public JarTransformer(final ClassLoader loader, final List<File> sources, final List<ClassFileTransformer> classFileTransformers) {
	this.loader = loader;
	this.sources = sources;
	this.classFileTransformers = classFileTransformers;
    }

    public void transform(final File destination) throws IOException, IllegalClassFormatException {
	JarOutputStream destinationJar = null;

	try {
	    final FileOutputStream fos = new FileOutputStream(destination);
	    final BufferedOutputStream bos = new BufferedOutputStream(fos);
	    destinationJar = new JarOutputStream(bos);

	    addPreResource(destinationJar);

	    for (final File source : sources) {
		JarFile sourceJar = null;
		try {
		    sourceJar = new JarFile(source);

		    transformSource(sourceJar, destinationJar);

		} finally {
		    if (sourceJar != null) {
			sourceJar.close();
		    }
		}
	    }

	    addPostResource(destinationJar);
	} finally {
	    if (destinationJar != null) {
		destinationJar.close();
	    }
	}
    }

    protected void addPreResource(final JarOutputStream destinationJar) throws IOException, IllegalClassFormatException {

    }

    protected void addPostResource(final JarOutputStream destinationJar) throws IOException, IllegalClassFormatException {

    }

    protected void transformSource(final JarFile source, final JarOutputStream destinationJar) throws IOException, IllegalClassFormatException {
	final Enumeration<JarEntry> entries = source.entries();
	while (entries.hasMoreElements()) {
	    final JarEntry entry = entries.nextElement();

	    final String entryName = entry.getName();

	    final InputStream is = source.getInputStream(entry);

	    byte[] byteCode = convert(is);
	    byteCode = transformByteCode(entryName, byteCode);

	    final JarEntry jarEntry = new JarEntry(entryName);
	    destinationJar.putNextEntry(jarEntry);
	    destinationJar.write(byteCode);
	}
    }

    protected byte[] transformByteCode(final String resourceName, final byte[] byteCode) throws IOException, IllegalClassFormatException {
	if (classFileTransformers == null || classFileTransformers.isEmpty()) {
	    return byteCode;
	}

	byte[] byteCodeModified = byteCode;

	if (resourceName.endsWith(CLASS_EXTENSION)) {
	    final String className = resourceName.substring(0, resourceName.length() - CLASS_EXTENSION.length());
	    for (final ClassFileTransformer classFileTransformer : classFileTransformers) {
		byteCodeModified = classFileTransformer.transform(loader, className, null, null, byteCodeModified);
	    }

	    if (byteCode != byteCodeModified) {
		System.out.println("Transform : " + className);
	    }
	}
	return byteCodeModified;
    }

    protected final static byte[] convert(final InputStream inputStream) throws IOException {
	int nRead;
	final byte[] data = new byte[1024];

	final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

	while ((nRead = inputStream.read(data, 0, data.length)) != -1) {
	    buffer.write(data, 0, nRead);
	}

	buffer.flush();

	final byte[] bytecode = buffer.toByteArray();

	return bytecode;
    }
}
