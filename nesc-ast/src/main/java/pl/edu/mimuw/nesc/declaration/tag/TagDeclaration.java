package pl.edu.mimuw.nesc.declaration.tag;

import com.google.common.base.Objects;
import com.google.common.base.Optional;
import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.gen.TagRef;
import pl.edu.mimuw.nesc.declaration.CopyController;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.type.Type;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public abstract class TagDeclaration extends Declaration {

    /**
     * Name is absent for anonymous tags.
     */
    private final Optional<String> name;

    /**
     * Kind of the tag this object reflects. Never null.
     */
    private final StructKind kind;

    /**
     * Mangled name of the tag that is globally unique.
     */
    private final Optional<String> uniqueName;

    /**
     * Size of objects of this tag in bytes.
     */
    private Optional<Integer> size;

    /**
     * Alignment of objects of this tag in bytes.
     */
    private Optional<Integer> alignment;

    /**
     * Value indicating if this tag has been already checked for correctness
     * and if it is correct.
     */
    private Optional<Boolean> isCorrect;

    /**
     * Value indicating if the AST node with the definition of the tag has
     * been already transformed to support bit-fields in external unions.
     */
    private boolean isTransformed;

    protected TagDeclaration(Builder<? extends TagDeclaration> builder) {
        super(builder);
        this.name = builder.name;
        this.uniqueName = builder.uniqueName;
        this.kind = builder.kind;
        this.size = Optional.absent();
        this.alignment = Optional.absent();
        this.isCorrect = Optional.absent();
        this.isTransformed = false;
    }

    /**
     * Get the name of the tag this object reflects.
     *
     * @return Name of the tag.
     */
    public Optional<String> getName() {
        return name;
    }

    /**
     * Get the globally unique name of the tag this object represents.
     *
     * @return The mangled, unique name of the tag. The object is present if and
     *         only if the normal name is present.
     */
    public Optional<String> getUniqueName() {
        return uniqueName;
    }

    /**
     * Check if this tag declaration corresponds to a defined tag.
     *
     * @return <code>true</code> if and only if this tag declaration corresponds
     *         to a defined tag and it has information from its definition.
     */
    public abstract boolean isDefined();

    /**
     * Get the kind of the tag this declaration corresponds to.
     *
     * @return Kind of the tag.
     */
    public StructKind getKind() {
        return kind;
    }

    /**
     * Check if this declaration corresponds to an external tag.
     *
     * @return <code>true</code> if and only if this tag corresponds to an
     *         external type.
     */
    public boolean isExternal() {
        return getKind().isExternal();
    }

    /**
     * @return Newly created object that represents the type that this tag
     *         corresponds to.
     */
    public abstract Type getType(boolean constQualified, boolean volatileQualified);

    /**
     * @return AST node that this object reflects.
     */
    public abstract TagRef getAstNode();

    /**
     * Get the size of objects of this tag.
     *
     * @return Size in bytes of objects of this tag.
     * @throws IllegalStateException The size has not been set yet.
     */
    public int getSize() {
        checkState(size.isPresent(), "size has not been computed yet");
        return size.get();
    }

    /**
     * Get the alignment of objects of this tag.
     *
     * @return Alignment in bytes of objects of this tag.
     * @throws IllegalStateException Tha alignment has not been set yet.
     */
    public int getAlignment() {
        checkState(alignment.isPresent(), "alignment has not been computed yet");
        return alignment.get();
    }

    /**
     * Set the size and alignment of the tag type associated with this tag.
     *
     * @param size Size to set.
     * @param alignment Alignment to set.
     * @throws IllegalArgumentException Size or alignment is not positive.
     * @throws IllegalStateException Size and alignment have been already set.
     */
    public void setLayout(int size, int alignment) {
        // Zero size is allowed to support empty structures, a GCC extension.
        checkArgument(size >= 0, "size cannot be negative");
        checkArgument(alignment > 0, "alignment must be positive");
        checkState(!this.size.isPresent() && !this.alignment.isPresent(),
                "size and alignment have been already set");

        this.size = Optional.of(size);
        this.alignment = Optional.of(alignment);
    }

    /**
     * Own objects that represent contents of this tag. For an enumeration tag
     * it is owning constant declarations of the tag. For a field tag type
     * this involves owning all fields of the tag. If this object does not
     * represent a definition, calling this method has no effect.
     */
    public abstract void ownContents();

    /**
     * Check if the size and alignment of objects of this tag are known.
     *
     * @return <code>true</code> if and only if the size and alignment of
     *         objects of this tag are known.
     */
    public boolean hasLayout() {
        return size.isPresent() && alignment.isPresent();
    }

    /**
     * Check if this tag is fully correct.
     *
     * @return <code>true</code> if and only if this tag is fully correct. The
     *         object is absent if its correctness has not been yet fully
     *         checked.
     */
    public Optional<Boolean> isCorrect() {
        return isCorrect;
    }

    /**
     * Set the value indicating if this tag is correct.
     *
     * @param isCorrect Value to set.
     * @throws IllegalStateException The value has been already set.
     */
    public void setIsCorrect(boolean isCorrect) {
        checkState(!this.isCorrect.isPresent(), "the correctness has been already set");
        this.isCorrect = Optional.of(isCorrect);
    }

    /**
     * Check if the AST node with definition of the tag has been transformed
     * to support bit-fields in external structures and external unions.
     *
     * @return Value of the flag.
     */
    public boolean isTransformed() {
        return isTransformed;
    }

    /**
     * Raise the transformed flag that indicates if the definition of the tag
     * has been transformed to support bit-fields in external structures and
     * external unions.
     */
    public void transformed() {
        this.isTransformed = true;
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + Objects.hashCode(name);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        if (!super.equals(obj)) {
            return false;
        }
        final TagDeclaration other = (TagDeclaration) obj;
        return Objects.equal(this.name, other.name);
    }

    public abstract <R, A> R accept(Visitor<R, A> visitor, A arg);

    public interface Visitor<R, A> {
        R visit(AttributeDeclaration attribute, A arg);

        R visit(EnumDeclaration _enum, A arg);

        R visit(StructDeclaration struct, A arg);

        R visit(UnionDeclaration union, A arg);
    }

    @Override
    public final TagDeclaration deepCopy(CopyController controller) {
        throw new UnsupportedOperationException("use the copy controller directly to copy a tag declaration");
    }

    /**
     * Builder for a tag declaration.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    public abstract static class Builder<T extends TagDeclaration> extends Declaration.Builder<T> {
        /**
         * Variables to allow control of the building process.
         */
        private boolean kindSet;

        /**
         * Data needed to build a tag declaration.
         */
        private Optional<String> name = Optional.absent();
        private Optional<String> uniqueName = Optional.absent();
        private StructKind kind;

        protected Builder() {
        }

        /**
         * Set the name of a tag. It may be null and then no name will be used.
         *
         * @param name Name of a tag to set.
         * @return <code>this</code>
         */
        public Builder<T> name(String name, String uniqueName) {
            this.name = Optional.fromNullable(name);
            this.uniqueName = Optional.fromNullable(uniqueName);
            return this;
        }

        /**
         * Set the kind of the tag.
         *
         * @param kind Kind to set.
         * @throws IllegalStateException The tag has been already set.
         */
        protected void setKind(StructKind kind) {
            checkState(!kindSet, "the tag kind can be set exactly once");
            this.kind = kind;
            this.kindSet = true;
        }

        /**
         * Get the name of the tag that has been set (or not).
         *
         * @return Name of the tag that has been set.
         */
        protected Optional<String> getName() {
            return name;
        }

        @Override
        protected void validate() {
            super.validate();
            checkNotNull(name, "tag name cannot be null");
            checkNotNull(uniqueName, "unique name cannot be null");
            checkNotNull(kind, "tag kind must not be null");
            checkState(name.isPresent() == uniqueName.isPresent(),
                    "unique name must be set if and only if the name is set");
        }
    }
}
