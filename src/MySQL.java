package com.sema4var.test;

import java.sql.SQLException;
import java.util.Properties;

import de.hu.berlin.wbi.objects.DatabaseConnection;

public class MySQL extends DatabaseConnection{

	public MySQL(Properties property) {
		super(property);
	}

	
	/**
	 * USe for INSERT, DELETE, UPDATE
	 * @param query
	 */
	public int executeUpdate(String sql){
		int returnValue=-1;
		
		try {
			stmt = conn.createStatement();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			returnValue=stmt.executeUpdate(sql);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		try {
			stmt.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		return returnValue;
	}
}
