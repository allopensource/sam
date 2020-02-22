package com.sam.mdm.repo;

import java.util.List;

import com.sam.helper.RepoException;
import com.sam.mdm.model.SAGAModel;
import com.sam.mdm.model.Saga;
import com.sam.sec.model.StepModel;

public interface MDMRepo {
	public void createSAGA(SAGAModel sagaModel) throws RepoException;

	public void createSAGAStep(Saga saga) throws RepoException;

	public SAGAModel getSagaDetails(String saga) throws RepoException;

	public List<String> getSagaNames() throws RepoException;

	public List<Saga> getSortedSagas() throws RepoException;

	public void delete(StepModel stepModel) throws RepoException;
}
