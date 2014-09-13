package pl.edu.mimuw.nesc.ast.type;

/**
 * Reflects the <code>long int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class LongType extends SignedIntegerType {
    public static final int INTEGER_RANK = 20;

    public LongType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }

    @Override
    public final Type addQualifiers(boolean addConst, boolean addVolatile,
                                    boolean addRestrict) {
        return new LongType(addConstQualifier(addConst), addVolatileQualifier(addVolatile));
    }

    @Override
    public final int getIntegerRank() {
        return INTEGER_RANK;
    }

    @Override
    public final UnsignedLongType getUnsignedIntegerType() {
        return new UnsignedLongType(isConstQualified(), isVolatileQualified());
    }

    @Override
    public <R, A> R accept(TypeVisitor<R, A> visitor, A arg) {
        return visitor.visit(this, arg);
    }
}
