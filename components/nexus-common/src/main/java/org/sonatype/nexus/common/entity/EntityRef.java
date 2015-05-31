package org.sonatype.nexus.common.entity;

/**
 * {@link Entity} reference.
 *
 * @since 3.0
 */
public interface EntityRef<T extends Entity>
{
  T get();
}
