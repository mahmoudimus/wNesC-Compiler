package pl.edu.mimuw.nesc.problem.issue;

import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class IncompleteParameterTypeError extends ErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INCOMPLETE_PARAMETER_TYPE);
    public static final Code CODE = _CODE;

    private final Optional<String> parameterName;
    private final Type actualType;

    public IncompleteParameterTypeError(Optional<String> parameterName, Type actualType) {
        super(_CODE);

        checkNotNull(parameterName, "parameter name cannot be null");
        checkNotNull(actualType, "actual type cannot be null");

        this.parameterName = parameterName;
        this.actualType = actualType;
    }

    @Override
    public final String generateDescription() {
        return   parameterName.isPresent()
               ? format("Parameter '%s' in function definition has incomplete type '%s'",
                        parameterName.get(), actualType)
               : format("Parameter in function definition has incomplete type '%s'",
                        actualType);
    }
}
