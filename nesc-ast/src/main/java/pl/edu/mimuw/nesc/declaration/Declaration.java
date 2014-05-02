package pl.edu.mimuw.nesc.declaration;

import com.google.common.base.Objects;
import pl.edu.mimuw.nesc.ast.Location;
import pl.edu.mimuw.nesc.symboltable.Partition;

/**
 * @author Grzegorz Kołakowski <gk291583@students.mimuw.edu.pl>
 */
public abstract class Declaration {

    protected final Location location;
    /**
     * Partition should be set only for global declarations.
     */
    protected Partition partition;

    protected Declaration(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public Partition getPartition() {
        return partition;
    }

    public void setPartition(Partition partition) {
        this.partition = partition;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(location);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        final Declaration other = (Declaration) obj;
        return Objects.equal(this.location, other.location);
    }
}
