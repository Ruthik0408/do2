package itsdc.dolphin.do2;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.jpa.domain.Specification;

import itsdc.dolphin.models.Do2;

public class Do2SpecBuilder {
	
	private final List<Do2SearchCriteria> params;
	
	public Do2SpecBuilder() {
		params=new ArrayList<Do2SearchCriteria>();
	}
	
	public Do2SpecBuilder with(String key,String operation,Object value) {
		params.add(new Do2SearchCriteria(key,operation,value));
		return this;
	}
	
	public Specification<Do2> build(){
		
		if(params.size()==0) {
			return null;
		}
		
		List<Specification> specs=null;//params.stream().map(Do2Specification::new).collect(Collectors.toList());
		
		Specification result=specs.get(0);
		for(int i=1;i<params.size();i++) {
			result=params.get(i)!=null?Specification.where(result).or(specs.get(i)):Specification.where(result).and(specs.get(i));
		}
		
		
		
		return result;
		
	}

}
