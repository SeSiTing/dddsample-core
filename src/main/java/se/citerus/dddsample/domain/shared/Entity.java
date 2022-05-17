package se.citerus.dddsample.domain.shared;

/**
 * An entity, as explained in the DDD book.
 *  
 */
public interface Entity<T> {

  /**
   * Entities compare by identity, not by attributes.
   * 实体比较的是唯一标识，而不是属性
   * @param other The other entity.
   * @return true if the identities are the same, regardless of other attributes.
   */
  boolean sameIdentityAs(T other);

}
