package pl.edu.mimuw.nesc.analysis.type;

/**
 * Reflects the <code>short int</code> type.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ShortType extends SignedIntegerType {
    public ShortType(boolean constQualified, boolean volatileQualified) {
        super(constQualified, volatileQualified);
    }

    @Override
    public final boolean isCharacterType() {
        return false;
    }
}
