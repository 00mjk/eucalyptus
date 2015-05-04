/*************************************************************************
 * Copyright 2009-2014 Eucalyptus Systems, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; version 3 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses/.
 *
 * Please contact Eucalyptus Systems, Inc., 6755 Hollister Ave., Goleta
 * CA 93117, USA or visit http://www.eucalyptus.com/licenses/ if you need
 * additional information or have any questions.
 ************************************************************************/

package com.eucalyptus.blockstorage;

import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.jmock.Expectations;
import org.jmock.integration.junit4.JUnitRuleMockery;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import com.eucalyptus.blockstorage.async.ExpiredVolumeCleaner;
import com.eucalyptus.blockstorage.async.FailedVolumeCleaner;
import com.eucalyptus.blockstorage.async.VolumeDeleter;
import com.eucalyptus.blockstorage.entities.VolumeInfo;
import com.eucalyptus.blockstorage.util.StorageProperties;
import com.eucalyptus.entities.Entities;
import com.eucalyptus.entities.TransactionResource;

/**
 * Created by wesw on 6/20/14.
 */
public class VolumeCleanupTest {

  @Rule
  public JUnitRuleMockery context = new JUnitRuleMockery();

  @BeforeClass
  public static void setupClass() {
    try {
      BlockStorageUnitTestSupport.setupBlockStoragePersistenceContext();
      BlockStorageUnitTestSupport.setupAuthPersistenceContext();
      BlockStorageUnitTestSupport.initializeAuth(1, 1);
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  @After
  public void teardown() {
    BlockStorageUnitTestSupport.flushBlockStorageEntities();
  }

  @AfterClass
  public static void teardownClass() {
    BlockStorageUnitTestSupport.tearDownBlockStoragePersistenceContext();
    BlockStorageUnitTestSupport.tearDownAuthPersistenceContext();
  }

  @Test
  public void cleanDeletingVolumes() throws Exception {
    Calendar twoDaysAgo = Calendar.getInstance();
    twoDaysAgo.add(Calendar.HOUR, -48);

    VolumeInfo good = new VolumeInfo();
    good.setStatus(StorageProperties.Status.deleted.toString());
    good.setSize(new Integer(1));
    good.setUserName("unittestuser0");
    good.setVolumeId("vol-0000");
    good.setSnapshotId("snap-0000");
    good.setCreateTime(twoDaysAgo.getTime());
    good.setDeletionTime(twoDaysAgo.getTime());
    good.setZone("eucalyptus");

    VolumeInfo failOne = new VolumeInfo();
    failOne.setStatus(StorageProperties.Status.deleting.toString());
    failOne.setSize(new Integer(1));
    failOne.setUserName("unittestuser0");
    failOne.setVolumeId("vol-0001");
    failOne.setSnapshotId("snap-0001");
    failOne.setCreateTime(new Date());
    failOne.setZone("eucalyptus");

    VolumeInfo failTwo = new VolumeInfo();
    failTwo.setStatus(StorageProperties.Status.deleting.toString());
    failTwo.setSize(new Integer(1));
    failTwo.setUserName("unittestuser0");
    failTwo.setVolumeId("vol-0002");
    failTwo.setSnapshotId("snap-0002");
    failTwo.setCreateTime(new Date());
    failTwo.setZone("eucalyptus");

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      Entities.persist(good);
      Entities.persist(failOne);
      Entities.persist(failTwo);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        exactly(2).of(storageManager).deleteVolume(with(any(String.class)));
      }
    });

    // Test VolumeDeleter
    VolumeDeleter vct = new VolumeDeleter(storageManager);
    vct.run();

    List<VolumeInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      remaining = Entities.query(new VolumeInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected 3 VolumeInfos to still exist but found " + remaining.size(), remaining.size() == 3);

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo example = new VolumeInfo();
      example.setStatus(StorageProperties.Status.deleted.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context for deleted volumeinfos", remaining != null);
    assertTrue("expected 3 VolumeInfos with deleted status to exist but found " + remaining.size(), remaining.size() == 3);

    // Test ExpiredVolumeCleaner
    ExpiredVolumeCleaner evc = new ExpiredVolumeCleaner();
    evc.run();

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      remaining = Entities.query(new VolumeInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected 2 VolumeInfos to still exist but found " + remaining.size(), remaining.size() == 2);

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo example = new VolumeInfo();
      example.setStatus(StorageProperties.Status.deleted.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context for deleted volumeinfos", remaining != null);
    assertTrue("expected two VolumeInfos with deleted status to exist but found " + remaining.size(), remaining.size() == 2);
  }

  @Test
  public void cleanFailedVolumesTest() throws Exception {
    Calendar twoDaysAgo = Calendar.getInstance();
    twoDaysAgo.add(Calendar.HOUR, -48);

    VolumeInfo good = new VolumeInfo();
    good.setStatus(StorageProperties.Status.available.toString());
    good.setSize(new Integer(1));
    good.setUserName("unittestuser0");
    good.setVolumeId("vol-0000");
    good.setSnapshotId("snap-0000");
    good.setCreateTime(new Date());
    good.setZone("eucalyptus");

    VolumeInfo failOne = new VolumeInfo();
    failOne.setStatus(StorageProperties.Status.failed.toString());
    failOne.setSize(new Integer(1));
    failOne.setUserName("unittestuser0");
    failOne.setVolumeId("vol-0001");
    failOne.setSnapshotId("snap-0001");
    failOne.setCreateTime(new Date());
    failOne.setZone("eucalyptus");

    VolumeInfo failTwo = new VolumeInfo();
    failTwo.setStatus(StorageProperties.Status.failed.toString());
    failTwo.setSize(new Integer(1));
    failTwo.setUserName("unittestuser0");
    failTwo.setVolumeId("vol-0002");
    failTwo.setSnapshotId("snap-0002");
    failTwo.setCreateTime(new Date());
    failTwo.setZone("eucalyptus");
    failTwo.setDeletionTime(twoDaysAgo.getTime());

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      good = Entities.persist(good);
      failOne = Entities.persist(failOne);
      failTwo = Entities.persist(failTwo);
      tran.commit();
    }

    final LogicalStorageManager storageManager = context.mock(LogicalStorageManager.class);
    context.checking(new Expectations() {
      {
        oneOf(storageManager).cleanVolume(with(any(String.class)));
      }
    });

    // Test FailedVolumeCleaner
    FailedVolumeCleaner fvc = new FailedVolumeCleaner(storageManager);
    fvc.run();

    List<VolumeInfo> remaining;
    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      remaining = Entities.query(new VolumeInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected 3 VolumeInfos to still exist, but found " + remaining.size(), remaining.size() == 3);

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo example = new VolumeInfo();
      example.setStatus(StorageProperties.Status.failed.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context for deleted volumeinfos", remaining != null);
    assertTrue("expected two VolumeInfos with failed status to exist but found " + remaining.size(), remaining.size() == 2);

    // Test ExpiredVolumeCleaner
    ExpiredVolumeCleaner evc = new ExpiredVolumeCleaner();
    evc.run();

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      remaining = Entities.query(new VolumeInfo());
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected 2 VolumeInfos to still exist, but found " + remaining.size(), remaining.size() == 2);

    try (TransactionResource tran = Entities.transactionFor(VolumeInfo.class)) {
      VolumeInfo example = new VolumeInfo();
      example.setStatus(StorageProperties.Status.failed.toString());
      remaining = Entities.query(example);
      tran.commit();
    }

    assertTrue("expected to have a result set querying the eucalyptus_storage persistence context", remaining != null);
    assertTrue("expected one VolumeInfo in failed state to exist, but found " + remaining.size(), remaining.size() == 1);
    assertTrue("expected a valid deletionTime but got null", remaining.get(0).getDeletionTime() != null);
    assertTrue("expected snapshot id to be snap-0001 but got " + remaining.get(0).getSnapshotId(),
        remaining.get(0).getSnapshotId().equals("snap-0001"));
  }
}
