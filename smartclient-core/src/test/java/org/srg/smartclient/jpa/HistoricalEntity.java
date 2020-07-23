package org.srg.smartclient.jpa;

import com.fasterxml.jackson.annotation.JsonBackReference;

import javax.persistence.*;
import java.sql.Date;

//@IdClass(HistoricalEntity.HistoricalEntityId.class)
@MappedSuperclass
public class HistoricalEntity<P extends GenericEntity> extends GenericEntity<HistoricalEntity<P>> {

//    public static class HistoricalEntityId implements Serializable {
//        private int owner;
//        private Date fromDate;
//
//        public int getOwner() {
//            return owner;
//        }
//
//        public void setOwner(int ownerId) {
//            this.owner = ownerId;
//        }
//
//        public Date getFromDate() {
//            return fromDate;
//        }
//
//        public void setFromDate(Date fromDate) {
//            this.fromDate = fromDate;
//        }
//
//        @Override
//        public boolean equals(Object o) {
//            if (this == o) return true;
//            if (!(o instanceof HistoricalEntityId)) return false;
//            HistoricalEntityId that = (HistoricalEntityId) o;
//            return getOwner() == that.getOwner() &&
//                    getFromDate().equals(that.getFromDate());
//        }
//
//        @Override
//        public int hashCode() {
//            return Objects.hash(getOwner(), getFromDate());
//        }
//    }


    //    @Id
    @ManyToOne
    @JoinColumn(name = "owner", nullable = false)
    @JsonBackReference
    private P owner;

    //    @Id
    @Column(name="start_date", nullable = false)
    private Date startDate;

    @Column(name="end_date")
    private Date endDate;

    public P getOwner() {
        return owner;
    }

    public void setOwner(P owner) {
        this.owner = owner;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }
}
