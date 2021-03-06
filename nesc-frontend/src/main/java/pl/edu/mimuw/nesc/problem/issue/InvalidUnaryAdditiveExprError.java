package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static com.google.common.base.Preconditions.*;
import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.*;
import static pl.edu.mimuw.nesc.astwriting.Tokens.UnaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidUnaryAdditiveExprError extends UnaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_UNARY_ADDITIVE_EXPR);
    public static final Code CODE = _CODE;

    public InvalidUnaryAdditiveExprError(UnaryOp op, Type argType, Expression argExpr) {
        super(_CODE, op, argType, argExpr);
        checkArgument(op == UNARY_MINUS || op == UNARY_PLUS, "invalid unary additive operator");
    }

    @Override
    public String generateDescription() {
        if (!argType.isGeneralizedArithmeticType()) {
            return format("Operand '%s' of unary operator %s has type '%s' but expecting an arithmetic type",
                          ASTWriter.writeToString(argExpr), op, argType);
        }

        return format("Invalid operand '%s' for unary operator %s",
                      ASTWriter.writeToString(argExpr), op);
    }
}
