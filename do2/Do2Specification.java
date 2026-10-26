package itsdc.dolphin.do2;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.Specification;

import itsdc.dolphin.models.Do2;

public class Do2Specification implements Specification<Do2>{

	//private Do2SearchCriteria criteria;
	private List<Do2SearchCriteria> criteriaList=new ArrayList<>();
	
	public void add(Do2SearchCriteria criteria) {
		criteriaList.add(criteria);
	}

	public Do2Specification() {
		
	}
	 

	 

	@Override
	public Predicate toPredicate(Root<Do2> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
		// TODO Auto-generated method stub
		List<Predicate> predicates=new ArrayList<>();
		for(Do2SearchCriteria criteria:criteriaList) {
			switch(criteria.getOperation()) {
			case "=":
				predicates.add(builder.equal(root.get(criteria.getKey()), criteria.getValue()));
				break;
			case "equal":
				predicates.add(builder.equal(root.get(criteria.getKey()), criteria.getValue()));
				break;
			case "in":
				predicates.add(builder.in(root.get(criteria.getKey())).value(criteria.getValue()));
				break;
			case "like":
				predicates.add(builder.like(builder.lower(root.get(criteria.getKey())),"%"+ criteria.getValue().toString().toLowerCase()+"%"));
				break;
			}
		}
		 
		return builder.and(predicates.toArray(new Predicate[0]));
	}
	
}
