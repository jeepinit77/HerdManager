import { initializeApp } from "firebase/app";
import { getFirestore, enableIndexedDbPersistence } from "firebase/firestore";
import { getAuth } from "firebase/auth";

const firebaseConfig = {
  apiKey: "AIzaSyBPxHhQ4IffOhsrS9XW0rnobZnUkVEyDtM",
  authDomain: "herdmanager-8eeb7.firebaseapp.com",
  projectId: "herdmanager-8eeb7",
  storageBucket: "herdmanager-8eeb7.firebasestorage.app",
  messagingSenderId: "409213885610",
  appId: "1:409213885610:web:herdmanager", // We will use a dummy appId or ask the user to register a web app
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize Cloud Firestore and get a reference to the service
export const db = getFirestore(app);

// Enable offline persistence
enableIndexedDbPersistence(db).catch((err) => {
  if (err.code == 'failed-precondition') {
    console.warn('Multiple tabs open, persistence can only be enabled in one tab at a a time.');
  } else if (err.code == 'unimplemented') {
    console.warn('The current browser does not support all of the features required to enable persistence');
  }
});

// Initialize Firebase Authentication
export const auth = getAuth(app);

export default app;
