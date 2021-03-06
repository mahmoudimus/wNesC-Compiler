package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import java.util.LinkedList;
import java.util.List;
import pl.edu.mimuw.nesc.abi.Endianness;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.ast.gen.Expression;
import pl.edu.mimuw.nesc.ast.gen.GccAttribute;
import pl.edu.mimuw.nesc.ast.gen.Identifier;
import pl.edu.mimuw.nesc.astutil.predicates.ExternalBaseAttributePredicate;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.declaration.object.TypenameDeclaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.external.ExternalScheme;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.ErroneousIssue;
import pl.edu.mimuw.nesc.problem.issue.InvalidExternalBaseAttributeError;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Object responsible for analysis of GCC attributes 'nx_base_be' and
 * 'nx_base_le' that are applied to type definitions that define external base
 * types.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
final class ExternalBaseAttributeAnalyzer implements AttributeSmallAnalyzer {

    /**
     * Object that will be notified about detected errors.
     */
    private final ErrorHelper errorHelper;

    /**
     * Predicate that will be used to detect an external base attribute.
     */
    private final Predicate<Attribute> predicate;

    ExternalBaseAttributeAnalyzer(ErrorHelper errorHelper) {
        checkNotNull(errorHelper, "error helper cannot be null");
        this.errorHelper = errorHelper;
        this.predicate = new ExternalBaseAttributePredicate();
    }

    @Override
    public void analyzeAttribute(List<Attribute> attributes, Declaration declaration, Environment environment) {
        int invalidAttributesCount = 0, allAttributesCount = 0;
        Optional<ExternalScheme> externalScheme = Optional.absent();

        for (Attribute attribute : attributes) {
            if (predicate.apply(attribute)) {
                final GccAttribute gccAttribute = (GccAttribute) attribute;

                externalScheme = analyzeAttribute(gccAttribute.getArguments(),
                        gccAttribute.getName().getName(), gccAttribute.getLocation(),
                        gccAttribute.getEndLocation());

                if (!externalScheme.isPresent()) {
                    ++invalidAttributesCount;
                }
                ++allAttributesCount;
            }
        }

        if (allAttributesCount > 1 && invalidAttributesCount == 0) {
            errorHelper.error(attributes.get(0).getLocation(), attributes.get(attributes.size() - 1).getEndLocation(),
                    InvalidExternalBaseAttributeError.tooManyAttributes());
        } else if (allAttributesCount == 1 && invalidAttributesCount == 0) {
            if (!(declaration instanceof TypenameDeclaration)) {
                errorHelper.error(attributes.get(0).getLocation(), attributes.get(attributes.size() - 1).getEndLocation(),
                        InvalidExternalBaseAttributeError.appliedNotToTypedef());
            } else {
                final TypenameDeclaration typenameDeclaration = (TypenameDeclaration) declaration;
                if (typenameDeclaration.getDenotedType().isPresent()
                        && !typenameDeclaration.getDenotedType().get().isArithmetic()) {
                    errorHelper.error(attributes.get(0).getLocation(), attributes.get(attributes.size() - 1).getEndLocation(),
                            InvalidExternalBaseAttributeError.appliedNotToArithmeticType());
                } else {
                    typenameDeclaration.addExternalScheme(externalScheme.get());
                }
            }
        }
    }

    private Optional<ExternalScheme> analyzeAttribute(Optional<LinkedList<Expression>> parameters,
                String attributeName, Location startLoc, Location endLoc) {
        final ErroneousIssue error;

        if (!parameters.isPresent()) {
            error = InvalidExternalBaseAttributeError.parametersMissing(attributeName);
        } else if (parameters.get().size() != 1) {
            error = InvalidExternalBaseAttributeError.invalidParamsCount(attributeName,
                    parameters.get().size());
        } else if (!(parameters.get().getFirst() instanceof Identifier)) {
            error = InvalidExternalBaseAttributeError.identifierExpected(attributeName,
                    parameters.get().getFirst());
        } else {
            final Identifier paramValue = (Identifier) parameters.get().getFirst();
            final Endianness endianness;

            if (ExternalBaseAttributePredicate.getBigEndianName().equals(attributeName)) {
                endianness = Endianness.BIG_ENDIAN;
            } else if (ExternalBaseAttributePredicate.getLittleEndianName().equals(attributeName)) {
                endianness = Endianness.LITTLE_ENDIAN;
            } else {
                throw new RuntimeException("unexpected external base type attribute name '" + attributeName + "'");
            }

            return Optional.of(new ExternalScheme(endianness, paramValue.getName()));
        }

        errorHelper.error(startLoc, endLoc, error);
        return Optional.absent();
    }
}
