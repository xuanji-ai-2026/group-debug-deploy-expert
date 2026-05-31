//
//  BxAppTests.swift
//  BxAppTests
//
//  北极星AI商机获客系统 - iOS客户端
//  单元测试
//
//  Created by Liu Ming (EMP-IOS-001) on 2024-01-15
//  Copyright © 2024 北极星AI. All rights reserved.
//

import XCTest
@testable import BxApp

final class BxAppTests: XCTestCase {

    override func setUpWithError() throws {
        // Setup code before each test
    }

    override func tearDownWithError() throws {
        // Teardown code after each test
    }

    // MARK: - Model Tests
    
    func testUserModelDecoding() throws {
        let json = """
        {
            "id": "user_001",
            "username": "testuser",
            "nickname": "Test User",
            "phone": "13800138000",
            "userType": "normal",
            "status": "active",
            "balance": 1000,
            "level": 1
        }
        """.data(using: .utf8)!
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        let user = try decoder.decode(User.self, from: json)
        
        XCTAssertEqual(user.id, "user_001")
        XCTAssertEqual(user.username, "testuser")
        XCTAssertEqual(user.nickname, "Test User")
        XCTAssertEqual(user.phone, "13800138000")
        XCTAssertEqual(user.userType, .normal)
        XCTAssertEqual(user.status, .active)
    }
    
    func testLeadModelDecoding() throws {
        let json = """
        {
            "id": "lead_001",
            "leadNo": "L202401001",
            "customerName": "张三",
            "customerPhone": "13800138000",
            "sourcePlatform": "wechat_public",
            "intentionLevel": "high",
            "followUpStatus": "in_progress",
            "ownerId": "user_001",
            "tenantId": "tenant_001"
        }
        """.data(using: .utf8)!
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        let lead = try decoder.decode(Lead.self, from: json)
        
        XCTAssertEqual(lead.id, "lead_001")
        XCTAssertEqual(lead.customerName, "张三")
        XCTAssertEqual(lead.intentionLevel, .high)
        XCTAssertEqual(lead.followUpStatus, .inProgress)
    }
    
    func testTaskModelDecoding() throws {
        let json = """
        {
            "id": "task_001",
            "taskNo": "T202401001",
            "name": "截客任务",
            "type": "intercept",
            "status": "running",
            "priority": "high",
            "progress": 50,
            "totalSteps": 4,
            "completedSteps": 2,
            "creatorId": "user_001",
            "executorId": "user_001",
            "tenantId": "tenant_001"
        }
        """.data(using: .utf8)!
        
        let decoder = JSONDecoder()
        decoder.dateDecodingStrategy = .iso8601
        
        let task = try decoder.decode(Task.self, from: json)
        
        XCTAssertEqual(task.id, "task_001")
        XCTAssertEqual(task.name, "截客任务")
        XCTAssertEqual(task.type, .intercept)
        XCTAssertEqual(task.status, .running)
        XCTAssertEqual(task.progress, 50)
    }
    
    // MARK: - Validation Tests
    
    func testPhoneValidation() throws {
        let validPhones = ["13800138000", "13912345678", "18612345678"]
        let invalidPhones = ["123456", "abc123", "138001380001"]
        
        for phone in validPhones {
            XCTAssertTrue(isValidPhone(phone), "\(phone) should be valid")
        }
        
        for phone in invalidPhones {
            XCTAssertFalse(isValidPhone(phone), "\(phone) should be invalid")
        }
    }
    
    func testVerificationCodeValidation() throws {
        let validCodes = ["123456", "000000", "999999"]
        let invalidCodes = ["12345", "1234567", "abc123"]
        
        for code in validCodes {
            XCTAssertTrue(isValidCode(code), "\(code) should be valid")
        }
        
        for code in invalidCodes {
            XCTAssertFalse(isValidCode(code), "\(code) should be invalid")
        }
    }
    
    // MARK: - Helper Methods
    
    private func isValidPhone(_ phone: String) -> Bool {
        let phoneRegex = "^1[3-9]\\d{9}$"
        let predicate = NSPredicate(format: "SELF MATCHES %@", phoneRegex)
        return predicate.evaluate(with: phone)
    }
    
    private func isValidCode(_ code: String) -> Bool {
        return code.count == 6 && code.allSatisfy { $0.isNumber }
    }
}
