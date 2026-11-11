// SPDX-License-Identifier: MIT
pragma solidity ^0.8.20;

/**
 * @title MedChainAccessControl
 * @dev Patient-Controlled Medical Data Access Smart Contract
 * Every patient owns their records. Doctors can only view records if explicitly
 * granted active, non-expired access by the patient.
 */
contract MedChainAccessControl {

    enum AccessLevel { NONE, READ_ONLY, READ_WRITE }

    struct MedicalRecord {
        uint256 id;
        string recordType;        // DIAGNOSIS, LAB_RESULT, PRESCRIPTION, IMAGING
        string title;
        string encryptedIpfsHash; // Encrypted IPFS CID or Data URI
        string dataHash;          // SHA-256 hash of original data for integrity
        string encryptedKey;      // AES key encrypted with patient's public key
        uint256 createdAt;
    }

    struct AccessGrant {
        address doctor;
        AccessLevel level;
        uint256 grantedAt;
        uint256 expiresAt;        // 0 = no expiry
        bool active;
        string encryptedKeyForDoctor; // AES key encrypted with doctor's public key
        uint256[] allowedRecordIds;   // empty array = all records
    }

    struct PatientProfile {
        string name;
        string bloodType;
        bool registered;
        uint256 registeredAt;
    }

    struct DoctorProfile {
        string name;
        string specialization;
        string licenseNumber;
        bool registered;
        bool verified;
        uint256 registeredAt;
    }

    // State Variables
    address public contractOwner;
    mapping(address => PatientProfile) public patients;
    mapping(address => DoctorProfile) public doctors;
    address[] public doctorAddresses;

    // patient => records
    mapping(address => MedicalRecord[]) private patientRecords;
    
    // patient => doctor => grant
    mapping(address => mapping(address => AccessGrant)) private accessGrants;
    
    // patient => list of doctor addresses with grants
    mapping(address => address[]) private patientDoctorGrants;

    // Events (Audit Trail on-chain)
    event PatientRegistered(address indexed patient, string name, uint256 timestamp);
    event DoctorRegistered(address indexed doctor, string name, string specialization, string licenseNumber);
    event MedicalRecordAdded(address indexed patient, uint256 indexed recordId, string recordType, string title);
    event AccessGranted(address indexed patient, address indexed doctor, AccessLevel level, uint256 expiresAt);
    event AccessRevoked(address indexed patient, address indexed doctor, uint256 timestamp);
    event RecordAccessed(address indexed patient, address indexed doctor, uint256 indexed recordId, uint256 timestamp);

    modifier onlyRegisteredPatient() {
        require(patients[msg.sender].registered, "Sender is not a registered patient");
        _;
    }

    modifier onlyRegisteredDoctor() {
        require(doctors[msg.sender].registered, "Sender is not a registered doctor");
        _;
    }

    constructor() {
        contractOwner = msg.sender;
    }

    // ─── Registration ─────────────────────────────────────────────────────────

    function registerPatient(string calldata _name, string calldata _bloodType) external {
        require(!patients[msg.sender].registered, "Already registered as patient");
        patients[msg.sender] = PatientProfile({
            name: _name,
            bloodType: _bloodType,
            registered: true,
            registeredAt: block.timestamp
        });
        emit PatientRegistered(msg.sender, _name, block.timestamp);
    }

    function registerDoctor(
        string calldata _name,
        string calldata _specialization,
        string calldata _licenseNumber
    ) external {
        require(!doctors[msg.sender].registered, "Already registered as doctor");
        doctors[msg.sender] = DoctorProfile({
            name: _name,
            specialization: _specialization,
            licenseNumber: _licenseNumber,
            registered: true,
            verified: true,
            registeredAt: block.timestamp
        });
        doctorAddresses.push(msg.sender);
        emit DoctorRegistered(msg.sender, _name, _specialization, _licenseNumber);
    }

    // ─── Record Management (Patient) ─────────────────────────────────────────

    function addRecord(
        string calldata _recordType,
        string calldata _title,
        string calldata _encryptedIpfsHash,
        string calldata _dataHash,
        string calldata _encryptedKey
    ) external onlyRegisteredPatient returns (uint256) {
        uint256 newId = patientRecords[msg.sender].length;
        patientRecords[msg.sender].push(MedicalRecord({
            id: newId,
            recordType: _recordType,
            title: _title,
            encryptedIpfsHash: _encryptedIpfsHash,
            dataHash: _dataHash,
            encryptedKey: _encryptedKey,
            createdAt: block.timestamp
        }));

        emit MedicalRecordAdded(msg.sender, newId, _recordType, _title);
        return newId;
    }

    function getMyRecords() external view onlyRegisteredPatient returns (MedicalRecord[] memory) {
        return patientRecords[msg.sender];
    }

    function getPatientRecordCount(address _patient) external view returns (uint256) {
        return patientRecords[_patient].length;
    }

    // ─── Access Control (Patient Sovereignty) ────────────────────────────────

    function grantAccess(
        address _doctor,
        AccessLevel _level,
        uint256 _durationSeconds,
        string calldata _encryptedKeyForDoctor,
        uint256[] calldata _allowedRecordIds
    ) external onlyRegisteredPatient {
        require(doctors[_doctor].registered, "Target address is not a registered doctor");
        require(_level != AccessLevel.NONE, "Invalid access level");

        uint256 expires = _durationSeconds == 0 ? 0 : block.timestamp + _durationSeconds;

        accessGrants[msg.sender][_doctor] = AccessGrant({
            doctor: _doctor,
            level: _level,
            grantedAt: block.timestamp,
            expiresAt: expires,
            active: true,
            encryptedKeyForDoctor: _encryptedKeyForDoctor,
            allowedRecordIds: _allowedRecordIds
        });

        // Track doctor list for patient UI
        bool exists = false;
        for (uint256 i = 0; i < patientDoctorGrants[msg.sender].length; i++) {
            if (patientDoctorGrants[msg.sender][i] == _doctor) {
                exists = true;
                break;
            }
        }
        if (!exists) {
            patientDoctorGrants[msg.sender].push(_doctor);
        }

        emit AccessGranted(msg.sender, _doctor, _level, expires);
    }

    function revokeAccess(address _doctor) external onlyRegisteredPatient {
        require(accessGrants[msg.sender][_doctor].active, "No active grant exists for doctor");
        accessGrants[msg.sender][_doctor].active = false;
        emit AccessRevoked(msg.sender, _doctor, block.timestamp);
    }

    function checkAccess(
        address _patient,
        address _doctor,
        uint256 _recordId
    ) public view returns (bool hasAccess, AccessLevel level, string memory encryptedKey) {
        AccessGrant memory grant = accessGrants[_patient][_doctor];

        if (!grant.active) return (false, AccessLevel.NONE, "");
        if (grant.expiresAt != 0 && block.timestamp > grant.expiresAt) return (false, AccessLevel.NONE, "");

        // Check record scope if restricted
        if (grant.allowedRecordIds.length > 0) {
            bool recordAllowed = false;
            for (uint256 i = 0; i < grant.allowedRecordIds.length; i++) {
                if (grant.allowedRecordIds[i] == _recordId) {
                    recordAllowed = true;
                    break;
                }
            }
            if (!recordAllowed) return (false, AccessLevel.NONE, "");
        }

        return (true, grant.level, grant.encryptedKeyForDoctor);
    }

    // ─── Doctor Access ────────────────────────────────────────────────────────

    function accessRecord(
        address _patient,
        uint256 _recordId
    ) external onlyRegisteredDoctor returns (MedicalRecord memory record, string memory encryptedKeyForDoctor) {
        (bool allowed, , string memory key) = checkAccess(_patient, msg.sender, _recordId);
        require(allowed, "Access Denied: You do not have valid permission from patient");
        require(_recordId < patientRecords[_patient].length, "Record does not exist");

        emit RecordAccessed(_patient, msg.sender, _recordId, block.timestamp);
        return (patientRecords[_patient][_recordId], key);
    }

    function getPatientRecordsAsDoctor(address _patient) external view onlyRegisteredDoctor returns (MedicalRecord[] memory) {
        AccessGrant memory grant = accessGrants[_patient][msg.sender];
        require(grant.active, "No active grant from this patient");
        if (grant.expiresAt != 0) {
            require(block.timestamp <= grant.expiresAt, "Access grant has expired");
        }

        return patientRecords[_patient];
    }

    // ─── Views & Queries for Web Frontend ─────────────────────────────────────

    function getMyGrants() external view onlyRegisteredPatient returns (AccessGrant[] memory) {
        uint256 count = patientDoctorGrants[msg.sender].length;
        AccessGrant[] memory list = new AccessGrant[](count);
        for (uint256 i = 0; i < count; i++) {
            address doc = patientDoctorGrants[msg.sender][i];
            list[i] = accessGrants[msg.sender][doc];
        }
        return list;
    }

    function getGrantForDoctor(address _patient, address _doctor) external view returns (AccessGrant memory) {
        return accessGrants[_patient][_doctor];
    }

    function getAllDoctors() external view returns (address[] memory addrs, DoctorProfile[] memory profiles) {
        uint256 count = doctorAddresses.length;
        profiles = new DoctorProfile[](count);
        for (uint256 i = 0; i < count; i++) {
            profiles[i] = doctors[doctorAddresses[i]];
        }
        return (doctorAddresses, profiles);
    }
}
