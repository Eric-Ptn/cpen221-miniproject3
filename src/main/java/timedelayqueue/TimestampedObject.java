package timedelayqueue;

import java.sql.Timestamp;
import java.util.UUID;

/**
 * A TimeStampedObject is a simple interface
 * for objects that have a unique identifier
 * and a timestamp.
 *
 * <p>
 *     This interface can be implemented by other
 *     types that need to have a timestamp as well
 *     as a unique identifier.
 * </p>
 *
 * @author Sathish Gopalakrishnan
 * @version 1.0
 */

public interface TimestampedObject {

    /**
     * Obtain a unique object identifier
     *
     * @return the unique object identifier for this object
     */
    UUID getId();

    /**
     * Obtain the timestamp associated with this object
     *
     * @return the timestamp associated with this object
     */
    Timestamp getTimestamp();

}
