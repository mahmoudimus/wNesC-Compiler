package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.base.Preconditions;
import pl.edu.mimuw.nesc.ast.RID;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.declaration.object.FunctionDeclaration.FunctionType;

import java.util.LinkedList;
import java.util.List;

/**
 * Utilities for {@link TypeElement}.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public final class TypeElementUtils {

    private static final IsTypedefVisitor IS_TYPEDEF_VISITOR = new IsTypedefVisitor();

    /**
     * Checks whether type elements contains <tt>TYPEDEF</tt> keyword.
     *
     * @param elements list of type elements of declaration
     * @return <code>true</code> if list contains <tt>TYPEDEF</tt>
     */
    public static boolean isTypedef(LinkedList<TypeElement> elements) {
        Preconditions.checkNotNull(elements, "elements list cannot be null");
        for (TypeElement element : elements) {
            if (element.accept(IS_TYPEDEF_VISITOR, null)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets function type implied by given modifiers.
     *
     * @param modifiers declaration's modifiers
     * @return one of nesc function types or <code>normal</code> if it is
     * plain C function
     */
    public static FunctionType getFunctionType(List<TypeElement> modifiers) {
        // FIXME: temporary solution, this kind of information should be
        // kept in type object
        for (TypeElement element : modifiers) {
            if (element instanceof Rid) {
                final Rid rid = (Rid) element;
                if (rid.getId() == RID.COMMAND) {
                    return FunctionType.COMMAND;
                }
                if (rid.getId() == RID.EVENT) {
                    return FunctionType.EVENT;
                }
                if (rid.getId() == RID.TASK) {
                    return FunctionType.TASK;
                }
            }
        }
        return FunctionType.NORMAL;
    }

    private TypeElementUtils() {
    }

    private static class IsTypedefVisitor extends ExceptionVisitor<Boolean, Void> {

        public Boolean visitTypeElement(TypeElement elem, Void arg) {
            throw new IllegalStateException("TypeElement object must not be instantiated.");
        }

        public Boolean visitTypename(Typename elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitTypeofExpr(TypeofExpr elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitTypeofType(TypeofType elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitAttribute(Attribute elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitGccAttribute(GccAttribute elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitRid(Rid elem, Void arg) {
            return RID.TYPEDEF.equals(elem.getId());
        }

        public Boolean visitQualifier(Qualifier elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitTagRef(TagRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitStructRef(StructRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitAttributeRef(AttributeRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitUnionRef(UnionRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitEnumRef(EnumRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitNxStructRef(NxStructRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitNxUnionRef(NxUnionRef elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitNescAttribute(NescAttribute elem, Void arg) {
            return Boolean.FALSE;
        }

        public Boolean visitTargetAttribute(TargetAttribute elem, Void arg) {
            return Boolean.FALSE;
        }

    }

}
