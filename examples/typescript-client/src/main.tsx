import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App.tsx'
import './index.scss'
import {worker} from "./mocks/worker.ts";

worker.start().then(() => {
    ReactDOM.createRoot(document.getElementById('root')!).render(<React.StrictMode>
        <App/>
    </React.StrictMode>)
})



