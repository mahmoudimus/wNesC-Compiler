package pl.edu.mimuw.nesc.analysis;

import pl.edu.mimuw.nesc.ast.StructKind;
import pl.edu.mimuw.nesc.ast.TagRefSemantics;
import pl.edu.mimuw.nesc.ast.gen.*;
import pl.edu.mimuw.nesc.ast.type.FieldTagType;
import pl.edu.mimuw.nesc.ast.type.Type;
import pl.edu.mimuw.nesc.ast.util.DeclaratorUtils;
import pl.edu.mimuw.nesc.declaration.object.ConstantDeclaration;
import pl.edu.mimuw.nesc.declaration.tag.*;
import pl.edu.mimuw.nesc.declaration.tag.fieldtree.*;
import pl.edu.mimuw.nesc.environment.Environment;
import pl.edu.mimuw.nesc.environment.ScopeType;
import pl.edu.mimuw.nesc.problem.ErrorHelper;
import pl.edu.mimuw.nesc.problem.issue.*;
import pl.edu.mimuw.nesc.symboltable.SymbolTable;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static pl.edu.mimuw.nesc.analysis.AttributesAnalysis.checkCAttribute;
import static pl.edu.mimuw.nesc.analysis.TypesAnalysis.resolveDeclaratorType;
import static pl.edu.mimuw.nesc.ast.util.TypeElementUtils.getStructKind;
import static pl.edu.mimuw.nesc.problem.issue.RedefinitionError.RedefinitionKind;
import static pl.edu.mimuw.nesc.problem.issue.RedeclarationError.RedeclarationKind;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;

/**
 * Class that contains code responsible for the semantic analysis.
 *
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class TagsAnalysis {
    /**
     * Updates information in the given environment that is related to the given
     * tag reference. All detected errors are reported.
     *
     * @param tagReference Tag reference to process.
     * @param environment Environment to update with information related to
     *                    given tag.
     * @param isStandalone <code>true</code> if and only if the given tag
     *                     reference is standalone (the meaning of standalone
     *                     definition is written in the definition of
     *                     {@link TagRefVisitor#isStandalone TagRefVisitor} class).
     * @param errorHelper Object that will be notified about detected errors.
     * @throws NullPointerException One of the arguments is null
     *                              (except <code>isStandalone</code>).
     */
    public static void processTagReference(TagRef tagReference, Environment environment,
            boolean isStandalone, ErrorHelper errorHelper, SemanticListener semanticListener) {
        // Validate arguments
        checkNotNull(tagReference, "tag reference cannot be null");
        checkNotNull(environment, "environment cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        // Process tag references
        final TagRefVisitor tagRefVisitor = new TagRefVisitor(environment, isStandalone,
                errorHelper, semanticListener);
        tagReference.accept(tagRefVisitor, null);
    }

    /**
     * Creates a new <code>FieldDeclaration</code> object that corresponds to
     * the given field. The given field is associated with the created object
     * and is available with its <code>getDeclaration</code> method.
     */
    public static void makeFieldDeclaration(FieldDecl fieldDecl, Optional<Type> maybeBaseType,
            Environment environment, ErrorHelper errorHelper) {
        final Optional<Declarator> maybeDeclarator = fieldDecl.getDeclarator();
        final Optional<Expression> maybeBitField = fieldDecl.getBitfield();

        Optional<Type> fullType = maybeBaseType;
        Optional<String> name = Optional.absent();

        if (maybeDeclarator.isPresent()) {
            final Declarator declarator = maybeDeclarator.get();
            name = DeclaratorUtils.getDeclaratorName(declarator);

            if (maybeBaseType.isPresent()) {
                fullType = resolveDeclaratorType(declarator, environment, errorHelper,
                        maybeBaseType.get());
            }
        }

        // Create and acknowledge the field
        final FieldDeclaration newField = new FieldDeclaration(name, fieldDecl.getLocation(),
                fieldDecl.getEndLocation(), fullType, maybeBitField.isPresent());
        fieldDecl.setDeclaration(newField);
    }

    private static List<ConstantDeclaration> getEnumerators(EnumRef enumRef) {
        checkNotNull(enumRef, "enum reference cannot be null");
        checkArgument(enumRef.getSemantics() == TagRefSemantics.DEFINITION,
                "enumeration type must be defined");

        final List<ConstantDeclaration> result = new ArrayList<>();

        for (Declaration declaration : enumRef.getFields()) {
            if (!(declaration instanceof Enumerator)) {
                throw new RuntimeException("an enumerator is of class '"
                        + declaration.getClass().getCanonicalName() + "'");
            }

            final Enumerator enumerator = (Enumerator) declaration;
            result.add(enumerator.getDeclaration());
        }

        return result;
    }

    /**
     * Unconditionally traverse the fields of the given tag reference and build
     * its structure. However, it makes sense only for the definition of a tag.
     * The definition is checked afterwards and all detected errors are
     * reported.
     *
     * @param tagRef Tag reference with fields to flick through.
     * @return Structure of fields of the given tag reference.
     */
    private static List<TreeElement> getFieldTagStructure(TagRef tagRef, ErrorHelper errorHelper) {
        final FieldTagDefinitionVisitor visitor = new FieldTagDefinitionVisitor(errorHelper);
        for (Declaration declaration : tagRef.getFields()) {
            declaration.accept(visitor, null);
        }

        final List<TreeElement> result = visitor.elements;
        checkTagDefinition(result, getStructKind(tagRef), errorHelper);

        return result;
    }

    private static void checkTagDefinition(List<TreeElement> fieldsStructure, StructKind kind,
            ErrorHelper errorHelper) {
        // Validate arguments
        checkNotNull(fieldsStructure, "fields structure cannot be null");
        checkNotNull(kind, "kind of the tag cannot be null");
        checkNotNull(errorHelper, "error helper cannot be null");

        // Prepare
        final int size = fieldsStructure.size();
        final boolean flexibleMemberConditions =
                size > 1 && (kind == StructKind.STRUCT || kind == StructKind.NX_STRUCT);

        // Check the structure
        final FieldValidityVisitor visitor = new FieldValidityVisitor(errorHelper);
        for (int i = 0; i < size; ++i) {
            final boolean canBeFlexibleMember = flexibleMemberConditions && i == size - 1;
            fieldsStructure.get(i).accept(visitor, canBeFlexibleMember);
        }

    }

    /**
     * A visitor that adds information about encountered tags to the symbol
     * table. It should be used only on objects of classes derived from
     * <code>TypeElement</code>.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class TagRefVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Environment that will be modified by this Visitor.
         */
        private final Environment environment;

        /**
         * Semantic listener that will take generated events.
         */
        private final SemanticListener semanticListener;

        /**
         * Object that will be informed about each encountered error.
         */
        private final ErrorHelper errorHelper;

        /**
         * <p><code>true</code> if and only if the tag that will be encountered
         * by this Visitor has been declared in a declaration that contains no
         * declarators, e.g.:</p>
         * <ul>
         *    <li><code>struct S;</code></li>
         *    <li><code>union U;</code></li>
         *    <li><code>nx_struct S;</code></li>
         *    <li><code>nx_union U;</code></li>
         * </ul>
         */
        private final boolean isStandalone;

        private TagRefVisitor(Environment environment, boolean isStandalone, ErrorHelper errorHelper,
                SemanticListener semanticListener) {
            this.semanticListener = semanticListener;
            this.errorHelper = errorHelper;
            this.environment = environment;
            this.isStandalone = isStandalone;
        }

        @Override
        public Void visitStructRef(StructRef structRef, Void v) {
            processTagRef(structRef);
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef unionRef, Void v) {
            processTagRef(unionRef);
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef nxStructRef, Void v) {
            processTagRef(nxStructRef);
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef nxUnionRef, Void v) {
            processTagRef(nxUnionRef);
            return null;
        }

        @Override
        public Void visitAttributeRef(AttributeRef attrRef, Void v) {
            checkState(attrRef.getSemantics() != TagRefSemantics.OTHER, "attribute reference that is not definition of an attribute");
            checkState(attrRef.getName() != null, "name of an attribute in its definition is null");
            processTagRef(attrRef);
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef enumRef, Void v) {
            processTagRef(enumRef);
            return null;
        }

        private void processTagRef(TagRef tagRef) {
            if (tagRef.getName() == null) {
                // emit potential error
                emitGlobalNameEvent(tagRef);
                return;
            }

            if (tagRef.getSemantics() == TagRefSemantics.OTHER) {
                declare(tagRef);
            } else {
                define(tagRef);
            }
        }

        private void declare(TagRef tagRef) {

            final String name = tagRef.getName().getName();
            final StructKind kind = getStructKind(tagRef);
            final SymbolTable<TagDeclaration> tagsTable = environment.getTags();
            final TagPredicate predicate = new TagPredicate(kind, false);
            final boolean onlyCurrentScope = isStandalone || !tagsTable.contains(name);
            final Optional<Boolean> sameTag = tagsTable.test(name, predicate, onlyCurrentScope);
            assert onlyCurrentScope || sameTag.isPresent() : "unexpected result of a test on a tag in the symbol table during a declaration";

            if (!sameTag.isPresent()) {
                environment.getTags().add(name, TagDeclarationFactory.getInstance(tagRef, semanticListener, errorHelper));
                emitGlobalNameEvent(tagRef);
            } else if (!sameTag.get()) {
                tagRef.setIsInvalid(true);
                errorHelper.error(tagRef.getLocation(), tagRef.getEndLocation(),
                                  new ConflictingTagKindError(name));
            } else {
                tagRef.setUniqueName(tagsTable.get(name).get().getUniqueName());
                tagRef.setNestedInNescEntity(environment.isTagDeclaredInsideNescEntity(name));
                emitGlobalNameEvent(tagRef);
            }

            // Check the correctness of an enumeration tag declaration
            if (kind == StructKind.ENUM) {
                Optional<? extends ErroneousIssue> error = Optional.absent();
                if (!isStandalone && (!predicate.isDefined || !sameTag.isPresent())) {
                    error = Optional.of(new UndefinedEnumUsageError(name));
                } else if (isStandalone) {
                    error = Optional.of(new EnumForwardDeclarationError());
                }

                if (error.isPresent()) {
                    tagRef.setIsInvalid(true);
                    errorHelper.error(tagRef.getLocation(), tagRef.getEndLocation(), error.get());
                }
            }
        }

        private void define(TagRef tagRef) {
            // Prepare variables
            final String name = tagRef.getName().getName();
            final StructKind kind = getStructKind(tagRef);
            final SymbolTable<TagDeclaration> tagsTable = environment.getTags();
            final Optional<? extends TagDeclaration> oldDecl = tagsTable.get(name, true);

            if (!oldDecl.isPresent()) {
                tagsTable.add(name, TagDeclarationFactory.getInstance(tagRef, semanticListener, errorHelper));
                emitGlobalNameEvent(tagRef);
            } else {
                /* A tag declaration is present in the current scope with the
                   same name. */
                final TagDeclaration oldDeclPure = oldDecl.get();

                // Check the correctness of the definition
                Optional<? extends ErroneousIssue> error = Optional.absent();
                if (oldDeclPure.getKind() != kind) {
                    error = Optional.of(new ConflictingTagKindError(name));
                } else if (oldDeclPure.isDefined()) {
                    error = Optional.of(new RedefinitionError(name, RedefinitionKind.TAG));
                } else if (oldDeclPure.getAstNode().getSemantics() == TagRefSemantics.PREDEFINITION) {
                    error = Optional.of(new RedefinitionError(name, RedefinitionKind.NESTED_TAG));
                }
                if (error.isPresent()) {
                    tagRef.setIsInvalid(true);
                    if (tagRef.getSemantics() == TagRefSemantics.PREDEFINITION) {
                        errorHelper.error(tagRef.getLocation(), tagRef.getEndLocation(), error.get());
                    }
                    return;
                }

                // Update the declaration in the symbol table and emit global name
                switch (tagRef.getSemantics()) {
                    case DEFINITION:
                        DefinitionTransition.transit(oldDeclPure, tagRef, errorHelper);
                        emitGlobalNameEvent(tagRef);
                        break;
                    case PREDEFINITION:
                        PredefinitionNode.update(oldDeclPure, tagRef);
                        break;
                    default:
                        throw new RuntimeException("unexpected tag reference semantics");
                }

                // Set the unique name in the AST node if necessary
                if (tagRef.getUniqueName() == null) {
                    tagRef.setUniqueName(oldDeclPure.getUniqueName());
                }
            }
        }

        private void emitGlobalNameEvent(TagRef tagRef) {
            if (checkCAttribute(tagRef, environment, errorHelper)
                    || environment.getScopeType() == ScopeType.GLOBAL) {
                if (tagRef.getName() != null) {
                    semanticListener.globalName(tagRef.getUniqueName().get(), tagRef.getName().getName());
                }

                /* Handle names of enumeration constants from enumerations
                   with @C() attribute. Global enumeration constants are handled
                   in 'Declarations.makeEnumerator' method. Currently, @C()
                   attribute is forbidden for anonymous tags so constants from
                   anonymous enumerations have unmangled names only if they are
                   declared in the global scope. */
                if (environment.getScopeType() != ScopeType.GLOBAL
                        && getStructKind(tagRef) == StructKind.ENUM
                        && tagRef.getSemantics() == TagRefSemantics.DEFINITION) {
                    emitEnumeratorsGlobalNameEvents((EnumRef) tagRef);
                }
            }
        }

        /**
         * Unconditionally emits global name events for enumeration constants of
         * given enumerated type.
         *
         * @param enumDefinition Definition of an enumerated type (with
         *                       constants present).
         */
        private void emitEnumeratorsGlobalNameEvents(EnumRef enumDefinition) {
            for (Declaration enumDecl : enumDefinition.getFields()) {
                if (!(enumDecl instanceof Enumerator)) {
                    continue;
                }

                final Enumerator enumerator = (Enumerator) enumDecl;
                semanticListener.globalName(enumerator.getUniqueName(), enumerator.getName());
            }
        }

        /**
         * A class that allows testing information about the tags that are
         * currently in the symbol table.
         *
         * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
         */
        private class TagPredicate implements Predicate<TagDeclaration> {
            private boolean sameKind;
            private boolean isDefined;
            private boolean insideDefinition;
            private final StructKind expectedKind;
            private final boolean mustBeUndefined;

            private TagPredicate(StructKind expectedKind, boolean mustBeUndefined) {
                checkNotNull(expectedKind, "expected kind in a tag predicate cannot be null");
                this.expectedKind = expectedKind;
                this.mustBeUndefined = mustBeUndefined;
            }

            @Override
            public boolean apply(TagDeclaration decl) {
                sameKind = decl.getKind() == expectedKind;
                isDefined = decl.isDefined();
                insideDefinition = decl.getAstNode().getSemantics() == TagRefSemantics.PREDEFINITION;
                return sameKind && (!mustBeUndefined || !isDefined && !insideDefinition);
            }
        }
    }

    /**
     * Visitor class that accumulates information about a field tag definition.
     * It expects to visit <code>DataDecl</code> objects. One instance of this
     * object shall be used to examine only one field tag definition.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class FieldTagDefinitionVisitor extends ExceptionVisitor<Void, Void> {
        /**
         * Object that will be notified about detected errors and warnings.
         */
        private final ErrorHelper errorHelper;

        /**
         * Set with names of fields that have been already acknowledged.
         */
        private final Set<String> fieldsNames = new HashSet<>();

        /**
         * Consecutive elements of the analyzed field tag.
         */
        private final List<TreeElement> elements = new ArrayList<>();

        private FieldTagDefinitionVisitor(ErrorHelper errorHelper) {
            this.errorHelper = errorHelper;
        }

        @Override
        public Void visitDataDecl(DataDecl dataDecl, Void v) {
            final List<Declaration> declarations = dataDecl.getDeclarations();

            // Determine the base type of elements declared in this declaration
            final Optional<Type> maybeBaseType = dataDecl.getType();
            checkState(maybeBaseType != null, "base type in a DataDecl object is null");

            // Process new fields
            if (!declarations.isEmpty()) {
                for (Declaration declaration : declarations) {
                    declaration.accept(this, null);
                }
            } else {
                /* Check if it is an unnamed field of an unnamed field tag type.
                   If so, add fields from it to this structure. */
                if (maybeBaseType.isPresent()) {
                    final Type baseType = maybeBaseType.get();

                    if (baseType.isFieldTagType()) {
                        final FieldTagType fieldTagType = (FieldTagType) baseType;
                        final FieldTagDeclaration fieldDecl = fieldTagType.getDeclaration();

                        if (!fieldDecl.getName().isPresent()) {
                            final Optional<List<TreeElement>> maybeStructure = fieldDecl.getStructure();
                            if (maybeStructure.isPresent()) {
                                appendElement(new BlockElement(maybeStructure.get(), fieldTagType.getBlockType()));
                            }
                        }
                    }
                }
            }

            return null;
        }

        @Override
        public Void visitFieldDecl(FieldDecl fieldDecl, Void v) {
            final FieldDeclaration fieldDeclaration = fieldDecl.getDeclaration();
            checkState(fieldDeclaration != null, "a FieldDecl object is not " +
                       "associated with its FieldDeclaration object");
            appendElement(new FieldElement(fieldDeclaration));
            return null;
        }

        @Override
        public Void visitExtensionDecl(ExtensionDecl extDecl, Void v) {
            extDecl.getDeclaration().accept(this, null);
            return null;
        }

        @Override
        public Void visitErrorDecl(ErrorDecl errorDecl, Void v) {
            return null;
        }

        private void appendElement(BlockElement element) {
            // Process all new fields
            final Set<String> newNestedNames = new HashSet<>();

            for (FieldDeclaration field : element) {
                final Optional<String> maybeName = field.getName();

                if (maybeName.isPresent()) {
                    final String name = maybeName.get();

                    if (fieldsNames.contains(name) && !newNestedNames.contains(name)) {
                        errorHelper.error(
                                field.getLocation(),
                                field.getEndLocation(),
                                new RedeclarationError(name, RedeclarationKind.FIELD)
                        );
                    }

                    newNestedNames.add(name);
                }
            }

            // Acknowledge the element
            fieldsNames.addAll(newNestedNames);
            elements.add(element);
        }

        private void appendElement(FieldElement element) {
            final FieldDeclaration fieldDeclaration = element.getFieldDeclaration();
            final Optional<String> maybeName = fieldDeclaration.getName();

            if (maybeName.isPresent()) {
                final String name = maybeName.get();

                if (fieldsNames.contains(name)) {
                    errorHelper.error(
                            fieldDeclaration.getLocation(),
                            fieldDeclaration.getEndLocation(),
                            new RedeclarationError(name, RedeclarationKind.FIELD)
                    );
                }

                fieldsNames.add(name);
            }

            elements.add(element);
        }
    }

    /**
     * Visitor that checks if a field is valid.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class FieldValidityVisitor implements TreeElement.Visitor<Void, Boolean> {
        /**
         * Object that will be notified about detected errors and warnings.
         */
        private final ErrorHelper errorHelper;

        private FieldValidityVisitor(ErrorHelper errorHelper) {
            checkNotNull(errorHelper, "error helper cannot be null");
            this.errorHelper = errorHelper;
        }

        @Override
        public Void visit(FieldElement element, Boolean canBeFlexibleMember) {
            final FieldDeclaration field = element.getFieldDeclaration();
            final Optional<Type> maybeType = field.getType();
            if (!maybeType.isPresent()) {
                return null;
            }
            final Type type = maybeType.get();

            // Check if the field is the flexible member
            if (canBeFlexibleMember && !type.isComplete() && type.isArrayType()) {
                return null;
            }

            if (type.isFunctionType() || !type.isComplete()) {
                errorHelper.error(
                        field.getLocation(),
                        field.getEndLocation(),
                        new InvalidFieldTypeError(type)
                );
            }

            return null;
        }

        @Override
        public Void visit(BlockElement blockElement, Boolean canBeFlexibleMember) {
            return null;
        }
    }

    /**
     * <p>An object responsible for creating concrete subclasses of
     * <code>TagDeclaration</code>.</p>
     * <p>Side effects of creation of a tag declaration object:</p>
     * <ul>
     *     <li>making the created tag declaration object point to the given tag
     *     reference</li>
     *     <li>making the given tag reference point to the created tag
     *     declaration object</li>
     *     <li>if the given tag reference represents a definition, checking the
     *     definition and emitting found issues</li>
     *     <li>if the given tag reference is named, generating a unique name for
     *     it using the name mangler and storing the name in the returned object
     *     and in the given tag reference</li>
     * </ul>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    static class TagDeclarationFactory extends ExceptionVisitor<TagDeclaration, Void> {
        private final ErrorHelper errorHelper;
        private final SemanticListener semanticListener;

        public static TagDeclaration getInstance(TagRef tagRef, SemanticListener semanticListener,
                ErrorHelper errorHelper) {
            checkNotNull(tagRef, "tag reference cannot be null");
            checkNotNull(semanticListener, "semantic listener cannot be null");
            checkNotNull(errorHelper, "error helper cannot be null");

            final TagDeclarationFactory factory = new TagDeclarationFactory(semanticListener, errorHelper);
            return tagRef.accept(factory, null);
        }

        private TagDeclarationFactory(SemanticListener semanticListener, ErrorHelper errorHelper) {
            this.semanticListener = semanticListener;
            this.errorHelper = errorHelper;
        }

        @Override
        public AttributeDeclaration visitAttributeRef(AttributeRef attrRef, Void arg) {
            AttributeDeclaration.Builder builder;

            if (attrRef.getSemantics() == TagRefSemantics.DEFINITION) {
                builder = AttributeDeclaration.definitionBuilder();
                builder.structure(getFieldTagStructure(attrRef, errorHelper));
            } else {
                builder = AttributeDeclaration.preDefinitionBuilder();
            }

            final String name = attrRef.getName().getName();
            final AttributeDeclaration result = builder
                    .astNode(attrRef)
                    .name(name, semanticListener.nameManglingRequired(name))
                    .startLocation(attrRef.getLocation())
                    .build();

            attrRef.setDeclaration(result);
            attrRef.setUniqueName(result.getUniqueName());

            return result;
        }

        @Override
        public StructDeclaration visitStructRef(StructRef structRef, Void arg) {
            return makeStructDeclaration(structRef, false);
        }

        @Override
        public StructDeclaration visitNxStructRef(NxStructRef nxStructRef, Void arg) {
            return makeStructDeclaration(nxStructRef, true);
        }

        @Override
        public EnumDeclaration visitEnumRef(EnumRef enumRef, Void arg) {
            EnumDeclaration.Builder builder;

            if (enumRef.getSemantics() != TagRefSemantics.DEFINITION) {
                builder = EnumDeclaration.declarationBuilder();
            } else {
                builder = EnumDeclaration.definitionBuilder()
                            .addAllEnumerators(getEnumerators(enumRef));
            }

            final Optional<String> name = getTagName(enumRef);
            final Optional<String> uniqueName = name.isPresent()
                    ? Optional.of(semanticListener.nameManglingRequired(name.get()))
                    : Optional.<String>absent();
            final EnumDeclaration result = builder
                    .astNode(enumRef)
                    .name(name.orNull(), uniqueName.orNull())
                    .startLocation(enumRef.getLocation())
                    .build();

            enumRef.setDeclaration(result);
            enumRef.setUniqueName(result.getUniqueName());

            return result;
        }

        @Override
        public UnionDeclaration visitUnionRef(UnionRef unionRef, Void arg) {
            return makeUnionDeclaration(unionRef, false);
        }

        @Override
        public UnionDeclaration visitNxUnionRef(NxUnionRef nxUnionRef, Void arg) {
            return makeUnionDeclaration(nxUnionRef, true);
        }

        private StructDeclaration makeStructDeclaration(StructRef structRef, boolean isExternal) {
            StructDeclaration.Builder builder;

            if (structRef.getSemantics() == TagRefSemantics.DEFINITION) {
                builder = StructDeclaration.definitionBuilder();
                builder.structure(getFieldTagStructure(structRef, errorHelper));
            } else {
                builder = StructDeclaration.declarationBuilder();
            }

            final Optional<String> name = getTagName(structRef);
            final Optional<String> uniqueName = name.isPresent()
                    ? Optional.of(semanticListener.nameManglingRequired(name.get()))
                    : Optional.<String>absent();
            final StructDeclaration result = builder
                    .isExternal(isExternal)
                    .astNode(structRef)
                    .name(name.orNull(), uniqueName.orNull())
                    .startLocation(structRef.getLocation())
                    .build();

            structRef.setDeclaration(result);
            structRef.setUniqueName(result.getUniqueName());

            return result;
        }

        private UnionDeclaration makeUnionDeclaration(UnionRef unionRef, boolean isExternal) {
            UnionDeclaration.Builder builder;

            if (unionRef.getSemantics() == TagRefSemantics.DEFINITION) {
                builder = UnionDeclaration.definitionBuilder();
                builder.structure(getFieldTagStructure(unionRef, errorHelper));
            } else {
                builder = UnionDeclaration.declarationBuilder();
            }

            final Optional<String> name = getTagName(unionRef);
            final Optional<String> uniqueName = name.isPresent()
                    ? Optional.of(semanticListener.nameManglingRequired(name.get()))
                    : Optional.<String>absent();
            final UnionDeclaration result = builder
                    .isExternal(isExternal)
                    .astNode(unionRef)
                    .name(name.orNull(), uniqueName.orNull())
                    .startLocation(unionRef.getLocation())
                    .build();

            unionRef.setDeclaration(result);
            unionRef.setUniqueName(result.getUniqueName());

            return result;
        }

        private Optional<String> getTagName(TagRef tagRef) {
            final String name =   tagRef.getName() != null
                                ? tagRef.getName().getName()
                                : null;
            return Optional.fromNullable(name);
        }
    }

    /**
     * <p>Class responsible for updating tag declarations by storing the
     * definition data in them.</p>
     * <p>Effects of a transition operation:</p>
     * <ul>
     *     <li>encapsulating information about definition of the given tag in
     *     the given tag declaration object</li>
     *     <li>making the given tag reference point to the given tag declaration
     *     object</li>
     *     <li>checking the definition of the tag and emitting found issues</li>
     * </ul>
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class DefinitionTransition extends ExceptionVisitor<Void, Void> {
        private final ErrorHelper errorHelper;
        private final TagDeclaration tagDeclaration;

        static void transit(TagDeclaration tagDeclaration, TagRef tagRef, ErrorHelper errorHelper) {
            checkNotNull(tagDeclaration, "tag declaration cannot be null");
            checkNotNull(tagRef, "tag reference cannot be null");
            checkNotNull(errorHelper, "error helper cannot be null");
            checkArgument(tagRef.getSemantics() == TagRefSemantics.DEFINITION,
                    "expecting a tag reference with definition");
            checkArgument(tagDeclaration.getAstNode() == tagRef,
                    "updating a tag declaration not associated with given tag reference");

            final DefinitionTransition visitor = new DefinitionTransition(tagDeclaration, errorHelper);
            tagRef.accept(visitor, null);
        }

        private DefinitionTransition(TagDeclaration tagDeclaration, ErrorHelper errorHelper) {
            this.tagDeclaration = tagDeclaration;
            this.errorHelper = errorHelper;
        }

        @Override
        public Void visitAttributeRef(AttributeRef attrRef, Void arg) {
            final AttributeDeclaration attrDecl = (AttributeDeclaration) tagDeclaration;
            attrDecl.define(getFieldTagStructure(attrRef, errorHelper));
            attrRef.setDeclaration(attrDecl);
            return null;
        }

        @Override
        public Void visitStructRef(StructRef structRef, Void arg) {
            updateStructDeclaration(structRef);
            return null;
        }

        @Override
        public Void visitNxStructRef(NxStructRef nxStructRef, Void arg) {
            updateStructDeclaration(nxStructRef);
            return null;
        }

        @Override
        public Void visitUnionRef(UnionRef unionRef, Void arg) {
            updateUnionDeclaration(unionRef);
            return null;
        }

        @Override
        public Void visitNxUnionRef(NxUnionRef nxUnionRef, Void arg) {
            updateUnionDeclaration(nxUnionRef);
            return null;
        }

        @Override
        public Void visitEnumRef(EnumRef enumRef, Void arg) {
            final EnumDeclaration enumDecl = (EnumDeclaration) tagDeclaration;
            enumDecl.define(getEnumerators(enumRef));
            enumRef.setDeclaration(enumDecl);
            return null;
        }

        private void updateStructDeclaration(StructRef structRef) {
            final StructDeclaration structDecl = (StructDeclaration) tagDeclaration;
            structDecl.define(getFieldTagStructure(structRef, errorHelper));
            structRef.setDeclaration(structDecl);
        }

        private void updateUnionDeclaration(UnionRef unionRef) {
            final UnionDeclaration unionDecl = (UnionDeclaration) tagDeclaration;
            unionDecl.define(getFieldTagStructure(unionRef, errorHelper));
            unionRef.setDeclaration(unionDecl);
        }
    }

    /**
     * Visitor that sets the pre-definition node of a tag declaration. Its only
     * effect is to update the pre-definition node in the given tag reference.
     *
     * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
     */
    private static class PredefinitionNode implements TagDeclaration.Visitor<Void, Void> {
        private final TagRef astNode;

        static void update(TagDeclaration tagDecl, TagRef tagRef) {
            checkNotNull(tagDecl, "tag declaration cannot be null");
            checkNotNull(tagRef, "tag reference cannot be null");
            tagDecl.visit(new PredefinitionNode(tagRef), null);
        }

        private PredefinitionNode(TagRef tagRef) {
            this.astNode = tagRef;
        }

        @Override
        public Void visit(AttributeDeclaration attrDecl, Void arg) {
            attrDecl.setPredefinitionNode((AttributeRef) astNode);
            return null;
        }

        @Override
        public Void visit(StructDeclaration structDecl, Void arg) {
            structDecl.setPredefinitionNode((StructRef) astNode);
            return null;
        }

        @Override
        public Void visit(UnionDeclaration unionDecl, Void arg) {
            unionDecl.setPredefinitionNode((UnionRef) astNode);
            return null;
        }

        @Override
        public Void visit(EnumDeclaration enumDecl, Void arg) {
            enumDecl.setPredefinitionNode((EnumRef) astNode);
            return null;
        }
    }
}
