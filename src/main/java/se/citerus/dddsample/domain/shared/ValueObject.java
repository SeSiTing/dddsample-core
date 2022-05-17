package se.citerus.dddsample.domain.shared;

import java.io.Serializable;

/**
 * A value object, as described in the DDD book.
 * 代表一个值对象
 */
public interface ValueObject<T> extends Serializable {

  /**
   * Value objects compare by the values of their attributes, they don't have an identity.
   * 值对象通过其属性的值进行比较，它们没有标识，判断给定值对象和这个值对象的属性是否相同
   * @param other The other value object.
   * @return <code>true</code> if the given value object's and this value object's attributes are the same.
   */
  boolean sameValueAs(T other);

}
