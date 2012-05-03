package org.sonatype.appcontext;

import java.util.Map;

import org.sonatype.appcontext.lifecycle.AppContextLifecycleManager;

/**
 * The generic app context, which is actually a Map. For modification, you can use only the {@link #put(String, Object)}
 * method, {@link #remove(Object)} method, {@link #putAll(Map)} and {@link #clear()} methods, since all the "collection"
 * methods like {@link #keySet()} {@link #values()} and {@link #entrySet()} returns unmodifiable "views" only!
 * 
 * @author cstamas
 */
public interface AppContext
    extends Map<String, Object>
{
    /**
     * A key to be used for mapping whenever needed, to find AppContext. For SISU/Guice support see
     * {@link AppContextModule} class. This key is merely to be used in Map-like mappings.
     */
    String APPCONTEXT_KEY = AppContext.class.getName();

    /**
     * Returns the time stamp in milliseconds when this context was created.
     * 
     * @return the creation time in milliseconds.
     */
    long getCreated();

    /**
     * Returns the time stamp in milliseconds when this context was last modified.
     * 
     * @return the creation time in milliseconds.
     */
    long getModified();

    /**
     * Returns the "generation" of the app context, usable for change detection. It is guaranteed, that when a change
     * happens against this context ({@link #put(String, Object)}, {@link #putAll(Map)}, {@link #remove(Object)} methods
     * are invoked), the integer returned by this method will be different than it was returned before the invocations
     * of changing methods. This method is better suited for change detection, as the {@link #getModified()} might
     * "oversee" changes happening in same millisecond.
     * 
     * @return
     * @since 3.2
     */
    int getGeneration();

    /**
     * Returns the id of this context.
     * 
     * @return
     */
    String getId();

    /**
     * Returns the parent app context if any, or {@code null} if this context is root context.
     * 
     * @return
     */
    AppContext getParent();

    /**
     * Returns the context's lifecycle manager.
     * 
     * @since 3.1
     */
    AppContextLifecycleManager getLifecycleManager();

    /**
     * Flattens this AppContext (calculates "visible" entries from this and it's parent and returns a plain Map. This
     * map is not connected to AppContext anymore, and not modifiable! It is just a "snapshot".
     * 
     * @return
     */
    Map<String, Object> flatten();

    /**
     * Interpolates passed in string using this app context as source.
     * 
     * @return
     * @since 3.0
     */
    String interpolate( String input )
        throws AppContextInterpolationException;

    /**
     * Returns the entry value, used in creation of this context. Gives access to source marker and raw (uninterpolated)
     * values. Low level method!
     * 
     * @return
     */
    AppContextEntry getAppContextEntry( String key );

    /**
     * Flattens this AppContext (calculates "visible" entries from this and it's parent and returns a plain Map but with
     * AppContextEntries as values. This map is not connected to AppContext anymore, and not modifiable! It is just a
     * "snapshot". Low level method!
     * 
     * @return
     */
    Map<String, AppContextEntry> flattenAppContextEntries();
}
