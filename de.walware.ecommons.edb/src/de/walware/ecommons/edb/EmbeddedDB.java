/*=============================================================================#
 # Copyright (c) 2009-2016 Stephan Wahlbrink (WalWare.de) and others.
 # All rights reserved. This program and the accompanying materials
 # are made available under the terms of the Eclipse Public License v1.0
 # which accompanies this distribution, and is available at
 # http://www.eclipse.org/legal/epl-v10.html
 # 
 # Contributors:
 #     Stephan Wahlbrink - initial API and implementation
 #=============================================================================*/

package de.walware.ecommons.edb;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.SQLException;
import java.util.Properties;

import javax.sql.DataSource;

import org.apache.commons.dbcp2.ConnectionFactory;
import org.apache.commons.dbcp2.DriverConnectionFactory;
import org.apache.commons.dbcp2.PoolableConnection;
import org.apache.commons.dbcp2.PoolableConnectionFactory;
import org.apache.commons.dbcp2.PoolingDataSource;
import org.apache.commons.pool2.ObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.derby.jdbc.EmbeddedDriver;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import de.walware.ecommons.edb.internal.Activator;


public class EmbeddedDB {
	
	
	private static abstract class AbstractDB {
		
		
		private final String dbLabel;
		
		
		public AbstractDB(final String dbLabel) {
			this.dbLabel= dbLabel;
		}
		
		
		public DataSource createConnectionPool(final String uri) throws CoreException {
			// see also PoolingDriverExample of DBCP
			final ConnectionFactory connectionFactory= createConnectionFactory(uri);
			final PoolableConnectionFactory poolableFactory= new PoolableConnectionFactory(connectionFactory, null);
			poolableFactory.setDefaultReadOnly(false);
			poolableFactory.setDefaultAutoCommit(true);
			final ObjectPool<PoolableConnection> connectionPool= new GenericObjectPool<>(poolableFactory);
			poolableFactory.setPool(connectionPool);
			return new PoolingDataSource<>(connectionPool);
		}
		
		public abstract ConnectionFactory createConnectionFactory(final String uri) throws CoreException;
		
		public void shutdown(final String uri) throws CoreException {
		}
		
		protected StringBuilder createErrorMessage(final String driverName, final Driver driver,
				final String uri, final String task) {
			final StringBuilder message= new StringBuilder("An error occurred when " + task + " embedded DB");
			message.append(" (" + this.dbLabel + " + DBCP)"); //$NON-NLS-1$ //$NON-NLS-2$
			message.append("\n\tDB ConnectionURL=").append(uri); //$NON-NLS-1$
			message.append("\n\tDriver Name=").append(driverName); //$NON-NLS-1$
			if (driver != null) {
				message.append(", Version=").append(driver.getMajorVersion()).append('.').append(driver.getMinorVersion()); //$NON-NLS-1$
			}
			return message;
		}
		
		
		@Override
		public String toString() {
			return this.dbLabel;
		}
		
	}
	
	private static class DerbyDB extends AbstractDB {
		
		private static String DERBY_HOME_PROP= "derby.system.home"; //$NON-NLS-1$
		private static String DRIVER_NAME= "org.apache.derby.jdbc.EmbeddedDriver"; //$NON-NLS-1$
		
		
		public DerbyDB() {
			super("Derby"); //$NON-NLS-1$
		}
		
		
		@Override
		public ConnectionFactory createConnectionFactory(final String uri) throws CoreException {
			if (System.getProperty(DERBY_HOME_PROP) == null) {
				final IPath location= Activator.getDefault().getStateLocation();
				System.setProperty(DERBY_HOME_PROP, location.toOSString());
			}
			
			final String driverName= DRIVER_NAME;
			final String dbUrl= "jdbc:derby:" + uri; //$NON-NLS-1$
			final Properties info= new Properties();
			info.setProperty("create", "true"); //$NON-NLS-1$ //$NON-NLS-2$
			Driver driver= null;
			try {
//				driver= (Driver) Class.forName(driverName, true, EmbeddedDB.class.getClassLoader()).newInstance();
				driver= new EmbeddedDriver();
				
				final Connection connection= driver.connect(dbUrl, info);
				connection.close();
				
				return new DriverConnectionFactory(driver, "jdbc:derby:" + uri, null); //$NON-NLS-1$
			}
			catch (final Exception e) {
				final StringBuilder message= createErrorMessage(driverName, driver, uri, "loading");
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, message.toString(), e));
			}
		}
		
		@Override
		public void shutdown(final String uri) throws CoreException {
			if (System.getProperty(DERBY_HOME_PROP) == null) {
				return;
			}
			final String driverName= DRIVER_NAME;
			final String dbUrl= "jdbc:derby:" + uri + ";shutdown=true"; //$NON-NLS-1$ //$NON-NLS-2$
			Driver driver= null;
			try {
//				driver= (Driver) Class.forName(driverName, true, EmbeddedDB.class.getClassLoader()).newInstance();
				driver= new EmbeddedDriver();
				
				final Connection connection= driver.connect(dbUrl, null);
				connection.close();
			}
			catch (final Exception e) {
				if (e instanceof SQLException && "08006".equals(((SQLException) e).getSQLState())) { //$NON-NLS-1$
					return;
				}
				final StringBuilder message= createErrorMessage(driverName, driver, uri, "closing");
				throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, -1, message.toString(), e));
			}
			
		}
		
	}
	
	
	private static final AbstractDB db= new DerbyDB();
	
	
	public static DataSource createConnectionPool(final String uri) throws CoreException {
		return db.createConnectionPool(uri);
	}
	
	public static ConnectionFactory createConnectionFactory(final String uri) throws CoreException {
		return db.createConnectionFactory(uri);
	}
	
	public static void shutdown(final String uri) throws CoreException {
		db.shutdown(uri);
	}
	
}
