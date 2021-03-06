package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.*;
import static pl.edu.mimuw.nesc.astwriting.Tokens.BinaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidEqualityExprError extends BinaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_EQUALITY_EXPR);
    public static final Code CODE = _CODE;

    public InvalidEqualityExprError(Type leftType, Expression leftExpr, BinaryOp op,
            Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, op, rightType, rightExpr);
        checkArgument(op == EQ || op == NE, "invalid equality operator");
    }

    @Override
    public String generateDescription() {
        return format("Invalid operands '%s' and '%s' of types '%s' and '%s' for operator %s",
                      ASTWriter.writeToString(leftExpr), ASTWriter.writeToString(rightExpr),
                      leftType, rightType, op);
    }
}
