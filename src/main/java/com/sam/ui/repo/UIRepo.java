package com.sam.ui.repo;

import java.util.List;

import com.sam.helper.RepoException;
import com.sam.sec.model.TxDataModel;

public interface UIRepo {
	public List<TxDataModel> getTransactions() throws RepoException;
}
