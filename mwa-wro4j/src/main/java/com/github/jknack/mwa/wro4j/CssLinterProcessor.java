package com.github.jknack.mwa.wro4j;

import static org.apache.commons.lang3.StringUtils.join;
import static org.apache.commons.lang3.Validate.notNull;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import org.apache.commons.io.IOUtils;

import ro.isdc.wro.extensions.processor.support.ObjectPoolHelper;
import ro.isdc.wro.extensions.processor.support.csslint.CssLint;
import ro.isdc.wro.extensions.processor.support.csslint.CssLintError;
import ro.isdc.wro.extensions.processor.support.csslint.CssLintException;
import ro.isdc.wro.model.resource.Resource;
import ro.isdc.wro.model.resource.ResourceType;
import ro.isdc.wro.model.resource.SupportedResourceType;
import ro.isdc.wro.model.resource.locator.factory.UriLocatorFactory;
import ro.isdc.wro.model.resource.processor.ResourcePreProcessor;
import ro.isdc.wro.util.ObjectFactory;

/**
 * Similar to {@link CssLinterProcessor} but let to capture {@link CssLintError}
 * .
 *
 * @author edgar.espina
 * @since 0.1.2
 */
@SupportedResourceType(ResourceType.CSS)
class CssLinterProcessor implements ResourcePreProcessor,
    UriLocatorFactoryAware {

  /**
   * The uri locator factory.
   */
  private UriLocatorFactory uriLocatorFactory;

  /**
   * Options to use to configure jsHint.
   */
  private String[] options;

  /**
   * The pool's engine.
   */
  private ObjectPoolHelper<CssLint> enginePool;

  /**
   * Creates a new {@link CssLinterProcessor}.
   *
   * @param options The lint options. Required.
   */
  public CssLinterProcessor(final LintOptions options) {
    this.options = notNull(options, "The lint options are required.").build();
    enginePool = new ObjectPoolHelper<CssLint>(new ObjectFactory<CssLint>() {
      @Override
      public CssLint create() {
        return new CssLint();
      }
    });
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void process(final Resource resource, final Reader reader,
      final Writer writer) throws IOException {
    final String content = IOUtils.toString(reader);
    final CssLint cssLint = enginePool.getObject();
    try {
      cssLint.setOptions(join(options, ",")).validate(content);
    } catch (final CssLintException e) {
      onCssLintException(e, resource);
    } finally {
      // don't change the processed content no matter what happens.
      writer.write(content);
      IOUtils.closeQuietly(reader);
      IOUtils.closeQuietly(writer);
      enginePool.returnObject(cssLint);
    }
  }

  /**
   * Called when {@link CssLintException} is thrown. Allows subclasses to
   * re-throw this exception as a {@link RuntimeException} or handle it
   * differently.
   *
   * @param ex {@link CssLintException} which has occurred.
   * @param resource the processed resource which caused the exception.
   */
  protected void onCssLintException(final CssLintException ex,
      final Resource resource) {
    throw new RuntimeLinterException(resource.getUri(), WroHelper.safeRead(
        uriLocatorFactory, resource),
        options, ex.getErrors().toArray(new CssLintError[0]));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setUriLocatorFactory(final UriLocatorFactory uriLocatorFactory) {
    this.uriLocatorFactory = uriLocatorFactory;
  }

}
