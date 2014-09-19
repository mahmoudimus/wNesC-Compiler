package pl.edu.mimuw.nesc.ast.type;

/**
 * An artificial type of type definitions, e.g.
 * <code>typedef unsigned long long size_t;</code>.
 *
 * It follows the Singleton design pattern.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TypeDefinitionType extends ArtificialType {
    /**
     * The only instance of this class.
     */
    private static final TypeDefinitionType instance = new TypeDefinitionType();

    /**
     * @return The only instance of this class.
     */
    public static TypeDefinitionType getInstance() {
        return instance;
    }

    /**
     * Private constructor for the Singleton design pattern.
     */
    private TypeDefinitionType() {}

    @Override
    public final boolean isTypeDefinition() {
        return true;
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
