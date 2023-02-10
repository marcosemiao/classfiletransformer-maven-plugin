/*
 * Copyright 2015 Marco Semiao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package fr.ms.instrument.maven.plugin;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import fr.ms.instrument.JarTransformer;
import fr.ms.instrument.Logger;

/**
 *
 * @see <a href="http://marcosemiao4j.wordpress.com">Marco4J</a>
 *
 *
 * @author Marco Semiao
 *
 */
@Mojo(name = "transform", defaultPhase = LifecyclePhase.PACKAGE, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class TransformMojo extends AbstractMojo {

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Parameter
	private String[] classFileTransformers;

	@Override
	public void execute() throws MojoExecutionException, MojoFailureException {
		final String packaging = project.getPackaging();
		final Artifact artifact = project.getArtifact();

		try {

			if ("jar".equals(packaging) && (artifact != null) && (classFileTransformers != null)
					&& (classFileTransformers.length > 0)) {
				final ClassLoader cl = getClassloader();
				final List<ClassFileTransformer> transformers = getClassFileTransformers(cl);

				final File source = artifact.getFile();
				final File destination = new File(source.getParent(), "instrument.jar");

				final Logger log = new MavenLogger(getLog());
				final JarTransformer transform = new JarTransformer(log, cl, Arrays.asList(source), transformers);
				transform.transform(destination);

				final File sourceRename = new File(source.getParent(), "notransform-" + source.getName());

				source.renameTo(sourceRename);
				destination.renameTo(source);
			}
		} catch (final Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private ClassLoader getClassloader() throws MojoExecutionException {
		try {
			final List<String> compileClasspathElements = project.getCompileClasspathElements();
			final List<URL> classPathUrls = new ArrayList<URL>();

			for (final String path : compileClasspathElements) {
				classPathUrls.add(new File(path).toURI().toURL());
			}

			final URLClassLoader urlClassLoader = URLClassLoader.newInstance(
					classPathUrls.toArray(new URL[classPathUrls.size()]),
					Thread.currentThread().getContextClassLoader());

			return urlClassLoader;
		} catch (final Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}

	private List<ClassFileTransformer> getClassFileTransformers(final ClassLoader cl) throws MojoExecutionException {
		try {
			final List<String> compileClasspathElements = project.getCompileClasspathElements();
			final List<URL> classPathUrls = new ArrayList<URL>();

			for (final String path : compileClasspathElements) {
				classPathUrls.add(new File(path).toURI().toURL());
			}

			final List<ClassFileTransformer> liste = new ArrayList<ClassFileTransformer>(classFileTransformers.length);
			for (final String classFileTransformer : classFileTransformers) {
				final Class<?> clazz = cl.loadClass(classFileTransformer);
				final ClassFileTransformer element = (ClassFileTransformer) clazz.newInstance();
				liste.add(element);
			}

			return liste;
		} catch (final Exception e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
	}
}
