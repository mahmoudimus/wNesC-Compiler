package pl.edu.mimuw.nesc;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import java.util.List;
import pl.edu.mimuw.nesc.common.SchedulerSpecification;
import pl.edu.mimuw.nesc.names.mangling.NameMangler;
import pl.edu.mimuw.nesc.problem.NescIssue;

import java.util.Collection;

/**
 * Contains the result of parsing process for entire project.
 *
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 * @author Michał Ciszewski <michal.ciszewski@students.mimuw.edu.pl>
 */
public final class ProjectData {

    public static Builder builder() {
        return new Builder();
    }

    private final ImmutableMap<String, FileData> fileDatas;
    private final ImmutableList<String> defaultIncludeFiles;
    private final ImmutableMap<String, String> globalNames;
    private final ImmutableMap<String, String> combiningFunctions;
    private final Optional<FileData> rootFileData;
    private final ImmutableList<NescIssue> issues;
    private final NameMangler nameMangler;
    private final Optional<SchedulerSpecification> schedulerSpecification;

    private ProjectData(Builder builder) {
        builder.buildMaps();

        this.fileDatas = builder.fileDatas;
        this.defaultIncludeFiles = builder.defaultIncludedFilesBuilder.build();
        this.globalNames = builder.globalNames;
        this.combiningFunctions = builder.combiningFunctions;
        this.rootFileData = Optional.fromNullable(builder.rootFileData);
        this.issues = builder.issueListBuilder.build();
        this.nameMangler = builder.nameMangler;
        this.schedulerSpecification = builder.schedulerSpecification;
    }

    public ImmutableMap<String, FileData> getFileDatas() {
        return fileDatas;
    }

    /**
     * Get the list with names of files that are included by default for this
     * project. They are not necessarily present in the map of file datas of
     * this object.
     *
     * @return List with paths of files included by default.
     */
    public ImmutableList<String> getDefaultIncludeFiles() {
        return defaultIncludeFiles;
    }

    /**
     * Map with unique names of entities that are to be located in the global
     * scope mapped to their normal names as they appear in the code. The names
     * are for the entire project.
     *
     * @return Mapping of unique names of global entities to their normal names.
     */
    public ImmutableMap<String, String> getGlobalNames() {
        return globalNames;
    }

    /**
     * Get the map with unique names of type definitions as keys and names of
     * combining functions associated with them as values. The map contains
     * information about combining functions from the entire project.
     *
     * @return Mapping of unique names of typedefs to names of combining
     *         functions associated with them.
     */
    public ImmutableMap<String, String> getCombiningFunctions() {
        return combiningFunctions;
    }

    public Optional<FileData> getRootFileData() {
        return rootFileData;
    }

    public ImmutableList<NescIssue> getIssues() {
        return issues;
    }

    /**
     * <p>Get name mangler that has been used to mangle names in the NesC
     * application and can be used to generate new unique names for it.
     * If the frontend instance used to create this project data processes
     * files, then the state of the mangler can change.</p>
     *
     * <p>If this project contains some errors, then the returned value may be
     * <code>null</code>.</p>
     *
     * @return Name mangler for further unique names generation.
     */
    public NameMangler getNameMangler() {
        return nameMangler;
    }

    /**
     * <p>Get the specification of the scheduler to use for this NesC project.
     * It is absent if the scheduler has not been set by an appropriate option
     * to the compiler.</p>
     *
     * <p>If this project contains some errors, the returned scheduler may be
     * absent even if specified by a compiler option.</p>
     *
     * @return Specification of the scheduler to use for this NesC application.
     */
    public Optional<SchedulerSpecification> getSchedulerSpecification() {
        return schedulerSpecification;
    }

    /**
     * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
     */
    public static class Builder {

        private ImmutableMap.Builder<String, FileData> fileDataBuilder;
        private ImmutableList.Builder<String> defaultIncludedFilesBuilder;
        private ImmutableList.Builder<NescIssue> issueListBuilder;
        private FileData rootFileData;

        private ImmutableMap<String, FileData> fileDatas;
        private ImmutableMap<String, String> globalNames;
        private ImmutableMap<String, String> combiningFunctions;

        private NameMangler nameMangler;
        private Optional<SchedulerSpecification> schedulerSpecification = Optional.absent();

        public Builder() {
            this.fileDataBuilder = ImmutableMap.builder();
            this.defaultIncludedFilesBuilder = ImmutableList.builder();
            this.issueListBuilder = ImmutableList.builder();
        }

        public Builder addRootFileData(FileData fileData) {
            this.rootFileData = fileData;
            return this;
        }

        public Builder addFileDatas(Collection<FileData> fileDatas) {
            for (FileData data : fileDatas) {
                this.fileDataBuilder.put(data.getFilePath(), data);
            }
            return this;
        }

        public Builder addDefaultIncludeFiles(List<String> filePaths) {
            this.defaultIncludedFilesBuilder.addAll(filePaths);
            return this;
        }

        public Builder addIssue(NescIssue issue) {
            this.issueListBuilder.add(issue);
            return this;
        }

        public Builder addIssues(Collection<NescIssue> issues) {
            this.issueListBuilder.addAll(issues);
            return this;
        }

        public Builder nameMangler(NameMangler nameMangler) {
            this.nameMangler = nameMangler;
            return this;
        }

        public Builder schedulerSpecification(SchedulerSpecification schedulerSpec) {
            this.schedulerSpecification = Optional.fromNullable(schedulerSpec);
            return this;
        }

        public ProjectData build() {
            verify();
            return new ProjectData(this);
        }

        private void verify() {
        }

        private void buildMaps() {
            this.fileDatas = fileDataBuilder.build();

            final ImmutableMap.Builder<String, String> globalNamesBuilder = ImmutableMap.builder();
            final ImmutableMap.Builder<String, String> combiningFunctionsBuilder = ImmutableMap.builder();

            for (FileData fileData : this.fileDatas.values()) {
                globalNamesBuilder.putAll(fileData.getGlobalNames());
                combiningFunctionsBuilder.putAll(fileData.getCombiningFunctions());
            }

            this.globalNames = globalNamesBuilder.build();
            this.combiningFunctions = combiningFunctionsBuilder.build();
        }
    }
}
