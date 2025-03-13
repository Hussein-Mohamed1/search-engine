"use client"

import useSearchStore from "@/store/searchStore";
import {useRouter, useSearchParams} from "next/navigation";

export function SearchButtons() {

    const {query} = useSearchStore();
    const searchParams = useSearchParams();
    const {push} = useRouter();

    const onSearch = () => {
        const params = new URLSearchParams(searchParams);
        if (query) {
            params.set('q', query);
        } else {
            params.delete('q');
        }
        push(`/search?${params.toString()}`);
    }


    return (<div className="flex flex-row  space-x-4 text-xs">
        <button
            className="px-4 py-2 rounded-md bg-gray-200 text-black font-bold hover:bg-gray-300"
            onClick={onSearch}
        >
            Search
        </button>
        <button className="px-4 py-2 rounded-md bg-gray-200 text-black font-bold hover:bg-gray-300">
            I&#39;m Feeling Lucky
        </button>
    </div>)
}