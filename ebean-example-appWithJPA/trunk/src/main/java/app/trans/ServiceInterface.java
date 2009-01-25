package app.trans;

import java.io.IOException;

public interface ServiceInterface {

//	@Transactional(readOnly=true, 
//		isolation=TxIsolation.SERIALIZABLE)
//		,noRollbackFor={NullPointerException.class})
	public void runInTrans() throws IOException;
		
}
