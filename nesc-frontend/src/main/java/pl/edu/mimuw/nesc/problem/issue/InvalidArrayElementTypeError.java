package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.type.Type;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidArrayElementTypeError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_ARRAY_ELEMENT_TYPE);
    public static final Code CODE = _CODE;

    private final Type arrayElementType;

    public InvalidArrayElementTypeError(Type arrayElementType) {
        super(_CODE);
        checkNotNull(arrayElementType, "array element type cannot be null");
        this.arrayElementType = arrayElementType;
    }

    @Override
    public String generateDescription() {
        if (!arrayElementType.isComplete()) {
            return format("Cannot use an array type with an incomplete element type '%s'",
                          arrayElementType);
        } else if (arrayElementType.isFunctionType()) {
            return format("Cannot use an array type with a function element type '%s'",
                          arrayElementType);
        }

        return format("Invalid array element type '%s'", arrayElementType);
    }
}
