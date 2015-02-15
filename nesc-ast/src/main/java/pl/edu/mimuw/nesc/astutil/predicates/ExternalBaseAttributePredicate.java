package pl.edu.mimuw.nesc.astutil.predicates;

import com.google.common.base.Predicate;
import java.util.Collection;
import java.util.Iterator;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.GccAttribute;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * This predicate is fulfilled if and only if the attribute is an attribute that
 * marks an external base type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class ExternalBaseAttributePredicate implements Predicate<Attribute> {
    /**
     * Names of the attributes that specify external base types.
     */
    private static final String NAME_NX_BASE_BE = "nx_base_be";
    private static final String NAME_NX_BASE_LE = "nx_base_le";

    public static String getBigEndianName() {
        return NAME_NX_BASE_BE;
    }

    public static String getLittleEndianName() {
        return NAME_NX_BASE_LE;
    }

    @Override
    public boolean apply(Attribute attribute) {
        checkNotNull(attribute, "attribute cannot be null");

        if (!(attribute instanceof GccAttribute)) {
            return false;
        }

        final GccAttribute gccAttribute = (GccAttribute) attribute;
        return isExternalBaseName(gccAttribute.getName().getName());
    }

    /**
     * Check if one of the attributes from the given collection is an external
     * base attribute.
     *
     * @param attributes Collection of attributes.
     * @return <code>true</code> if and only if one of the attributes on the
     *         given collection is an external base attribute. In other words,
     *         <code>true</code> is returned if the predicate is fulfilled for
     *         an attribute from the collection.
     */
    public boolean apply(Collection<? extends Attribute> attributes) {
        checkNotNull(attributes, "attribute cannot be null");

        for (Attribute attribute : attributes) {
            if (apply(attribute)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Remove attributes from the given collection that satisfy this predicate.
     *
     * @param attributes Collection of attributes.
     * @return <code>true</code> if and only if the given collection has been
     *         modified, i.e. an attribute fulfilled this predicate.
     */
    public boolean remove(Collection<? extends Attribute> attributes) {
        checkNotNull(attributes, "attributes cannot be null");

        final Iterator<? extends Attribute> attributesIt = attributes.iterator();
        boolean fulfilled = false;

        while (attributesIt.hasNext()) {
            if (apply(attributesIt.next())) {
                attributesIt.remove();
                fulfilled = true;
            }
        }

        return fulfilled;
    }

    private boolean isExternalBaseName(String name) {
        return name.equals(NAME_NX_BASE_BE) || name.equals(NAME_NX_BASE_LE);
    }
}
