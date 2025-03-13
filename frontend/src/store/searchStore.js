// src/store/searchStore.js
import {create} from 'zustand';
import {createJSONStorage, persist} from "zustand/middleware";

const useSearchStore = create(
    persist(
        (set) => ({
            query: "",
            setQuery: (query) => set({query}),
        }),
        {
            name: 'query-storage', // unique name in storage
            storage: createJSONStorage(() => sessionStorage),
        }
    )
);

export default useSearchStore;