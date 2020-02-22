package com.sam.ui.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.sam.helper.RepoException;
import com.sam.sec.model.TxDataModel;
import com.sam.ui.repo.UIRepo;

@Service
public class UIService {
	@Autowired
	private UIRepo uiRepo;

	public List<TxDataModel> getTransactions() throws RepoException {
		return uiRepo.getTransactions();
	}
}
