package se.citerus.dddsample.domain.model.cargo;

import org.apache.commons.lang.Validate;

import se.citerus.dddsample.domain.model.handling.HandlingHistory;
import se.citerus.dddsample.domain.model.location.Location;
import se.citerus.dddsample.domain.shared.DomainObjectUtils;
import se.citerus.dddsample.domain.shared.Entity;

/**
 * A Cargo. This is the central class in the domain model,
 * and it is the root of the Cargo-Itinerary-Leg-Delivery-RouteSpecification aggregate.
 *
 * A cargo is identified by a unique tracking id, and it always has an origin
 * and a route specification. The life cycle of a cargo begins with the booking procedure,
 * when the tracking id is assigned. During a (short) period of time, between booking
 * and initial routing, the cargo has no itinerary.
 *
 * The booking clerk requests a list of possible routes, matching the route specification,
 * and assigns the cargo to one route. The route to which a cargo is assigned is described
 * by an itinerary.
 *
 * A cargo can be re-routed during transport, on demand of the customer, in which case
 * a new route is specified for the cargo and a new route is requested. The old itinerary,
 * being a value object, is discarded and a new one is attached.
 *
 * It may also happen that a cargo is accidentally misrouted, which should notify the proper
 * personnel and also trigger a re-routing procedure.
 *
 * When a cargo is handled, the status of the delivery changes. Everything about the delivery
 * of the cargo is contained in the Delivery value object, which is replaced whenever a cargo
 * is handled by an asynchronous event triggered by the registration of the handling event.
 *
 * The delivery can also be affected by routing changes, i.e. when the route specification
 * changes, or the cargo is assigned to a new route. In that case, the delivery update is performed
 * synchronously within the cargo aggregate.
 *
 * The life cycle of a cargo ends when the cargo is claimed by the customer.
 *
 * The cargo aggregate, and the entire domain model, is built to solve the problem
 * of booking and tracking cargo. All important business rules for determining whether
 * or not a cargo is misdirected, what the current status of the cargo is (on board carrier,
 * in port etc), are captured in this aggregate.
 *
 */
 // 货物聚合
public class Cargo implements Entity<Cargo> {
  // 跟踪号码
  private TrackingId trackingId;
  // 地址
  private Location origin;
  // 路线
  private RouteSpecification routeSpecification;
  // 行程
  private Itinerary itinerary;
  // 交货
  private Delivery delivery;

  /**
   * 初始化货物聚合，唯一id+入参对象
   */
  public Cargo(final TrackingId trackingId, final RouteSpecification routeSpecification) {
    // 在各个方法前添加前置校验
    Validate.notNull(trackingId, "Tracking ID is required");
    Validate.notNull(routeSpecification, "Route specification is required");

    this.trackingId = trackingId;
    // Cargo origin never changes, even if the route specification changes.
    // 即使路线规格发生变化，货物来源也不会改变
    // However, at creation, cargo orgin can be derived from the initial route specification.
    // 但是，在创建时，可以从初始路线规范派生货物来源
    this.origin = routeSpecification.origin();
    this.routeSpecification = routeSpecification;

    this.delivery = Delivery.derivedFrom(
      this.routeSpecification, this.itinerary, HandlingHistory.EMPTY
    );
  }

  /**
   * The tracking id is the identity of this entity, and is unique.
   * 
   * @return Tracking id.
   */
  public TrackingId trackingId() {
    return trackingId;
  }

  /**
   * @return Origin location.
   */
  public Location origin() {
    return origin;
  }

  /**
   * @return The delivery. Never null.
   */
  public Delivery delivery() {
    return delivery;
  }

  /**
   * @return The itinerary. Never null.
   */
  public Itinerary itinerary() {
    return DomainObjectUtils.nullSafe(this.itinerary, Itinerary.EMPTY_ITINERARY);
  }

  /**
   * @return The route specification.
   */
  public RouteSpecification routeSpecification() {
    return routeSpecification;
  }
  
  /**
   * Specifies a new route for this cargo.
   * 给此货物指定一个新路线
   * todo 由此可见在聚合根中，是其他实体也可以进行修改，也可以调用其他实体的内部方法
   * @param routeSpecification route specification.
   */
  public void specifyNewRoute(final RouteSpecification routeSpecification) {
    Validate.notNull(routeSpecification, "Route specification is required");

    this.routeSpecification = routeSpecification;
    // Handling consistency within the Cargo aggregate synchronously
    // 同步处理 Cargo 聚合中的一致性
    this.delivery = delivery.updateOnRouting(this.routeSpecification, this.itinerary);
  }

  /**
   * Attach a new itinerary to this cargo.
   *给此货物指定一个新的行程
   * @param itinerary an itinerary. May not be null.
   */
  public void assignToRoute(final Itinerary itinerary) {
    Validate.notNull(itinerary, "Itinerary is required for assignment");

    this.itinerary = itinerary;
    // Handling consistency within the Cargo aggregate synchronously
    // 同步处理 Cargo 聚合中的一致性
    this.delivery = delivery.updateOnRouting(this.routeSpecification, this.itinerary);
  }

  /**
   * Updates all aspects of the cargo aggregate status
   * based on the current route specification, itinerary and handling of the cargo.
   * <p/>
   * When either of those three changes, i.e. when a new route is specified for the cargo,
   * the cargo is assigned to a route or when the cargo is handled, the status must be
   * re-calculated.
   * <p/>
   * {@link RouteSpecification} and {@link Itinerary} are both inside the Cargo
   * aggregate, so changes to them cause the status to be updated <b>synchronously</b>,
   * but changes to the delivery history (when a cargo is handled) cause the status update
   * to happen <b>asynchronously</b> since {@link se.citerus.dddsample.domain.model.handling.HandlingEvent} is in a different aggregate.
   *
   * @param handlingHistory handling history
   */
  public void deriveDeliveryProgress(final HandlingHistory handlingHistory) {
    // TODO filter events on cargo (must be same as this cargo)

    // Delivery is a value object, so we can simply discard the old one
    // and replace it with a new
    // 由于Delivery是一个值对象，所以我们可以直接丢弃，然会创建一个新的
    this.delivery = Delivery.derivedFrom(routeSpecification(), itinerary(), handlingHistory);
  }

  /**
   * 判断id是否相同
   */
  @Override
  public boolean sameIdentityAs(final Cargo other) {
    return other != null && trackingId.sameValueAs(other.trackingId);
  }

  /**
   * @param object to compare
   * @return True if they have the same identity
   * @see #sameIdentityAs(Cargo)
   */
  @Override
  public boolean equals(final Object object) {
    if (this == object) return true;
    if (object == null || getClass() != object.getClass()) return false;

    final Cargo other = (Cargo) object;
    return sameIdentityAs(other);
  }

  /**
   * @return Hash code of tracking id.
   */
  @Override
  public int hashCode() {
    return trackingId.hashCode();
  }

  @Override
  public String toString() {
    return trackingId.toString();
  }

  Cargo() {
    // Needed by Hibernate
  }

  // Auto-generated surrogate key
  private Long id;

}
