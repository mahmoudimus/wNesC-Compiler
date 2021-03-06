package pl.edu.mimuw.nesc.problem.issue;

import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.type.PointerType;
import pl.edu.mimuw.nesc.type.Type;
import pl.edu.mimuw.nesc.astwriting.ASTWriter;

import static java.lang.String.format;
import static pl.edu.mimuw.nesc.astwriting.Tokens.BinaryOp.*;

/**
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class InvalidSimpleAssignExprError extends BinaryExprErroneousIssue {
    private static final ErrorCode _CODE = ErrorCode.onlyInstance(Issues.ErrorType.INVALID_SIMPLE_ASSIGN_EXPR);
    public static final Code CODE = _CODE;

    public InvalidSimpleAssignExprError(Type leftType, Expression leftExpr,
            Type rightType, Expression rightExpr) {
        super(_CODE, leftType, leftExpr, ASSIGN, rightType, rightExpr);
    }

    @Override
    public String generateDescription() {
        if (leftType.isFieldTagType()) {

            if (!leftType.removeQualifiers().isCompatibleWith(rightType.removeQualifiers())) {
                return format("Cannot assign to structure or union '%s' from '%s' of incompatible type '%s'",
                        ASTWriter.writeToString(leftExpr), ASTWriter.writeToString(rightExpr),
                        rightType);
            }

        } else if (leftType.isPointerType() && rightType.isPointerType()) {

            final PointerType leftPtrType = (PointerType) leftType,
                              rightPtrType = (PointerType) rightType;
            final Type leftRefType = leftPtrType.getReferencedType(),
                       rightRefType = rightPtrType.getReferencedType();

            if (!leftRefType.hasAllQualifiers(rightRefType)) {
                String qualifierDiscarded;
                if (rightRefType.isConstQualified() && !leftRefType.isConstQualified()) {
                    qualifierDiscarded = "const";
                } else if (rightRefType.isVolatileQualified() && !leftRefType.isVolatileQualified()) {
                    qualifierDiscarded = "volatile";
                } else {
                    qualifierDiscarded = "restrict";
                }

                return format("Cannot assign '%s' to '%s' because it discards '%s' qualifier of pointed type '%s' of the right operand",
                              ASTWriter.writeToString(rightExpr), ASTWriter.writeToString(leftExpr), qualifierDiscarded,
                              rightRefType);
            }
        }

        return format("Invalid assignment of '%s' of type '%s' to '%s' of type '%s'",
                      ASTWriter.writeToString(rightExpr), rightType,
                      ASTWriter.writeToString(leftExpr), leftType);
    }
}
