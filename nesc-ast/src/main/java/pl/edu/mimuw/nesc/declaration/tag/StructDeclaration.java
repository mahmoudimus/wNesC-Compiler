package pl.edu.mimuw.nesc.declaration.tag;

import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.StructRef;
import pl.edu.mimuw.nesc.type.ExternalStructureType;
import pl.edu.mimuw.nesc.type.FieldTagType;
import pl.edu.mimuw.nesc.type.StructureType;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public class StructDeclaration extends FieldTagDeclaration<StructRef> {
    /**
     * Get the builder for declarations of structure tags that are not
     * definitions.
     *
     * @return Newly created builder that will build an object that corresponds
     *         to a declaration of a structure type that is not definition.
     */
    public static Builder declarationBuilder() {
        return new Builder(FieldTagDeclaration.Builder.Kind.DECLARATION);
    }

    /**
     * Get a builder for a structure declaration that corresponds to a structure
     * definition but without the requirement of specifying its fields that is
     * present in the definition builder.
     *
     * @return Newly created pre-definition builder.
     */
    public static Builder preDefinitionBuilder() {
        return new Builder(FieldTagDeclaration.Builder.Kind.PREDEFINITION);
    }

    /**
     * Get the builder for a definition of a structure tag.
     *
     * @return Newly created builder that will build an object that corresponds
     *         to a definition of a structure type.
     */
    public static Builder definitionBuilder() {
        return new Builder(FieldTagDeclaration.Builder.Kind.DEFINITION);
    }

    /**
     * Initialize this structure declaration.
     *
     * @param builder Builder with necessary information.
     */
    private StructDeclaration(Builder builder) {
        super(builder);
    }

    @Override
    public FieldTagType<StructDeclaration> getType(boolean constQualified, boolean volatileQualified) {
        return   isExternal()
               ? new ExternalStructureType(constQualified, volatileQualified, this)
               : new StructureType(constQualified, volatileQualified, this);
    }

    @Override
    public <R, A> R accept(Visitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }

    /**
     * Builder for a struct declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public static final class Builder extends FieldTagDeclaration.ExtendedBuilder<StructRef, StructDeclaration> {

        private Builder(Kind builderKind) {
            super(builderKind);
        }

        @Override
        protected void beforeBuild() {
            super.beforeBuild();
            setKind(isExternal ? StructKind.NX_STRUCT : StructKind.STRUCT);
        }

        @Override
        protected StructDeclaration create() {
            return new StructDeclaration(this);
        }
    }
}
