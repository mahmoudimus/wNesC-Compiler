package pl.edu.mimuw.nesc.astbuilding;

import com.google.common.collect.ImmutableListMultimap;
import pl.edu.mimuw.nesc.issue.ErrorHelper;
import pl.edu.mimuw.nesc.issue.NescIssue;
import pl.edu.mimuw.nesc.token.Token;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class AstBuildingBase {

    protected final ErrorHelper errorHelper;
    protected final ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder;

    protected AstBuildingBase(ImmutableListMultimap.Builder<Integer, NescIssue> issuesMultimapBuilder,
                              ImmutableListMultimap.Builder<Integer, Token> tokensMultimapBuilder) {
        this.errorHelper = new ErrorHelper(issuesMultimapBuilder);
        this.tokensMultimapBuilder = tokensMultimapBuilder;
    }


}