package pl.edu.mimuw.nesc.analysis.attributes;

import com.google.common.collect.ImmutableList;
import java.util.List;
import pl.edu.mimuw.nesc.abi.ABI;
import pl.edu.mimuw.nesc.analysis.SemanticListener;
import pl.edu.mimuw.nesc.ast.gen.Attribute;
import pl.edu.mimuw.nesc.declaration.Declaration;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.problem.ErrorHelper;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * <p>Class of objects responsible for analyzing attributes and performing all
 * necessary actions associated with them.</p>
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class AttributeAnalyzer {
    /**
     * Analyzers that will perform the analysis.
     */
    private final ImmutableList<AttributeSmallAnalyzer> analyzersChain;

    public AttributeAnalyzer(ABI abi, SemanticListener semanticListener, ErrorHelper errorHelper) {
        checkNotNull(abi, "ABI cannot be null");
        checkNotNull(semanticListener, "semantic listener cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        this.analyzersChain = ImmutableList.of(
                new CombineAttributeAnalyzer(errorHelper, semanticListener),
                new CAttributeAnalyzer(errorHelper, semanticListener),
                new ExternalBaseAttributeAnalyzer(errorHelper),
                new CallInfoAttributesAnalyzer(new NescCallInfoAttributes(), errorHelper),
                new CallInfoAttributesAnalyzer(new GccCallInfoAttributes(), errorHelper),
                new GccInterruptAttributesAnalyzer(abi)
        );
    }

    public void analyzeAttributes(List<Attribute> attributes, Declaration declaration,
            Environment environment) {
        checkNotNull(attributes, "attributes list cannot be null");
        checkNotNull(declaration, "declaration cannot be null");
        checkNotNull(environment, "environment cannot be null");

        for (AttributeSmallAnalyzer smallAnalyzer : analyzersChain) {
            smallAnalyzer.analyzeAttribute(attributes, declaration, environment);
        }
    }
}
